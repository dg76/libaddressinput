package com.foursquare.geo.lib

import com.foursquare.common.scala.Identity._
import com.foursquare.common.scala.Lists.Implicits._
import com.foursquare.geo.gen.PostalAddress
import com.google.template.soy.internal.base.CharEscapers
import net.liftweb.json.JsonParser

case class AddressFormatterOptions(
  preferLatin: Boolean = false,
  includeCountry: Boolean = false
)

case class FormattedAddress(
  tokens: Seq[BaseAddressToken]
) {
  def toLines() = { tokens.map(_.s).mkString("").split("\n").filterNot(_.isEmpty).toVector }

  def valueHtmlEscapeHelper(specifier: String, rawValue: String): String = {
    val value = CharEscapers.htmlEscaper().escape(rawValue)

    AddressFormatter.tokenToSchemaMap.get(specifier) match {
      case Some(itemprop) if value.nonEmpty => "<span itemprop=\"%s\">%s</span>".format(itemprop, value)
      case _ => value
    }
  }

  def asHtml() = {
    tokens.map(_ match {
      case t: AddressToken => valueHtmlEscapeHelper(t.tokenType, t.s)
      case t => CharEscapers.htmlEscaper().escape(t.s)
    }).mkString("").split("\n").toVector
  }
}

sealed trait BaseAddressToken {
  def s: String
}
object AddressTokenNewLine extends BaseAddressToken { val s = "\n" }
case class AddressTokenPuncuation(val s: String) extends BaseAddressToken
case class AddressToken(val s: String, val tokenType: String) extends BaseAddressToken
case class MissingAddressToken(val token: String) extends BaseAddressToken { val s = "" }

case class AddressLineWithEntity(
  line: String,
  entity: Option[AddressEntity] = None
)

case class AddressEntity(
  entityType: String,
  value: String,
  position: Int
)

case class AddressFormat(
  abbr: Option[String],
  fmt: Option[String],
  lfmt: Option[String],
  lang: Option[String],
  languages: List[String],
  require: Option[String],
  state_name_type: Option[String],
  sub_keys: List[String],
  upper: Option[String],
  zip: Option[String],
  id: String,
  prefer_state_abbreviation: Option[Boolean] = Some(false),
  zip_output_fmt: Option[String]
) {
  val zipRegex = zip.map(zipRe => ("^" + zipRe + "$").r)
  lazy val gbReformatRegex = "([^ ]*)[ ]?(.{3})".r
  def zipMatches(z: String): Boolean = zipRegex.map(_.findFirstIn(z).isDefined).getOrElse(true)
  def reformatZip(z: String): String = {
    (zipRegex, zip_output_fmt) match {
      case (Some(inputRe), Some(outputFmt)) => inputRe.replaceAllIn(z, outputFmt)
      case _ => {
        // I couldn't get the regexp right to do the validation and the splitting
        if (id =? "GB") {
          gbReformatRegex.replaceAllIn(z, "$1 $2")
        } else {
          z
        }
      }
    }
  }

  def doFormat(postalAddress: PostalAddress, options: AddressFormatterOptions = AddressFormatterOptions()): FormattedAddress = {
    val ccFormat: Option[String] = {
      // We are not doing the right thing now knowing when to preferLatin, so we
      // are showing chinese address in english with no spaces and we look dumb.
      // CJK users are used to seeing english-style spaces in their addresses, so I'd
      // rather do that.
      if (options.preferLatin || true) {
        lfmt.orElse(fmt)
      } else {
        fmt
      }
    }

    val format: String = ccFormat match {
      case Some(fmt) if (ccFormat.exists(_.nonEmpty)) => fmt
      case _ => "{NAME}\n{ORGANIZATION}\n{STREET_ADDRESS}\n{CITY} {POSTAL_CODE}"
    }

    doFormat(format, postalAddress, options)
  }

  def getStreetValue(postalAddress: PostalAddress): Vector[BaseAddressToken] = {
    val streetOption = postalAddress.streetAddressOption
    val crossOption = postalAddress.crossStreetOption

    if (crossOption.exists(_.nonEmpty) && streetOption.exists(_.nonEmpty)) {
      Vector(
        AddressToken(streetOption.get, AddressFormatter.STREET_ADDRESS),
        AddressTokenPuncuation(" "),
        // hack to make sure the trailing ) doesn't get deleted for being at the end of the line
        AddressToken("(" + crossOption.get + ")", AddressFormatter.CROSS_STREET)
      )
    } else {
      streetOption.orElse(crossOption).toVector.filterNot(_.isEmpty).map(v => AddressToken(v, AddressFormatter.STREET_ADDRESS))
    }
  }

  def doFormat(formatStr: String, postalAddress: PostalAddress, options: AddressFormatterOptions): FormattedAddress = {
    // TODO(blackmad): pre-compute this
    val modifiedFormatStr = formatStr
      .replace(AddressFormatter.NAME_TOKEN, "")
      .replace(AddressFormatter.ORGANIZATION_TOKEN, "")
      .replace(AddressFormatter.SORTING_CODE_TOKEN, "")
      .replaceAll(" +", " ")
      .replaceAll("^\n+", "")

    val fmtTokenLines: Vector[Vector[String]] = modifiedFormatStr.split("\n").toVector.map(_.split("\\{|\\}").toVector.filterNot(_.isEmpty))
    val tokenLines: Vector[Vector[BaseAddressToken]] = fmtTokenLines.map(_.flatMap(token => {
      if (token.isEmpty) {
        Vector.empty
      } else if (token =? AddressFormatter.PROVINCE_ABBR) {
        PostalAddress.provinceCode.getter(postalAddress)
          .orElse(PostalAddress.province.getter(postalAddress)).toVector
          .map(v => AddressToken(v, AddressFormatter.PROVINCE))
      } else if (token =? AddressFormatter.STREET_ADDRESS) {
        getStreetValue(postalAddress)
      } else if (AddressFormatter.tokenToThriftMap.get(token).isDefined) {
        val fieldValue: Option[String] =
          AddressFormatter.tokenToThriftMap.get(token).flatMap(_.getter(postalAddress))

        fieldValue match {
          case Some(value) if value.nonEmpty => Vector(AddressToken(value, token))
          case _ => Vector() // MissingAddressToken(token))
        }

      } else {
        Vector(AddressTokenPuncuation(token))
      }
    }))

    val fixedTokens = (for {
      (line, lineIndex) <- tokenLines.zipWithIndex
      if line.nonEmpty && line.exists(_.isInstanceOf[AddressToken])
    } yield {
      def isDroppableToken(t: BaseAddressToken) = {
        lazy val hasPostalCodeOnLine = (line.collect {
          case AddressToken(value, token) => token =? AddressFormatter.POSTAL_CODE
        }).nonEmpty

        val isJapanesePostalSymbolAndShouldKeep =
          t.s =? "\u3012" && hasPostalCodeOnLine

        !t.isInstanceOf[AddressToken] && !isJapanesePostalSymbolAndShouldKeep
      }

      // drop non-address tokens at start and end
      val firstPass = line.dropWhile(isDroppableToken)
      val secondPass = firstPass.reverse.dropWhile(t => !t.isInstanceOf[AddressToken]).reverse

      val thirdPass = (secondPass match {
        case AddressToken(value, token) +: tail if token =? AddressFormatter.PROVINCE && tail.isEmpty => {
          // if the only token on this line is a province token, make sure we're using the expansion
          val province = PostalAddress.province.getter(postalAddress)
            .orElse(PostalAddress.provinceCode.getter(postalAddress)).getOrElse(throw new Exception("province disappeared"))
          Vector(AddressToken(province, AddressFormatter.PROVINCE))
        }
        case _ => secondPass
      })

      if (lineIndex !=? tokenLines.size - 1) {
        thirdPass :+ AddressTokenNewLine
      } else {
        thirdPass
      }
    }).flatten

    val finalTokens = fixedTokens ++ (postalAddress.countryOption match {
      case Some(c) if options.includeCountry => Vector(AddressTokenNewLine, AddressToken(c, AddressFormatter.COUNTRY))
      case _ => Vector.empty
    })

    FormattedAddress(finalTokens)
  }
}

object AddressFormatter extends AddressFormatterMixin {
  val unusedTokens = List('N', 'O')

  val STREET_ADDRESS = "STREET_ADDRESS"
  val CROSS_STREET = "CROSS_STREET"
  val COUNTRY = "COUNTRY"
  val POSTAL_CODE = "POSTAL_CODE"
  val SUBURB = "SUBURB"
  val CITY = "CITY"
  val PROVINCE = "PROVINCE"
  val ABBR = "+ABBR"
  val PROVINCE_ABBR = "PROVINCE" + ABBR
  val NAME = "NAME"
  val NAME_TOKEN = "{" + NAME + "}"
  val ORGANIZATION = "ORGANIZATION"
  val ORGANIZATION_TOKEN = "{" + ORGANIZATION + "}"
  val SORTING_CODE = "SORTING_CODE"
  val SORTING_CODE_TOKEN = "{" + SORTING_CODE + "}"

  val tokenToThriftMap = Map(
    POSTAL_CODE -> PostalAddress.postalCode,
    STREET_ADDRESS -> PostalAddress.streetAddress,
    SUBURB -> PostalAddress.neighborhood,
    CITY -> PostalAddress.city,
    PROVINCE -> PostalAddress.province,
    PROVINCE + ABBR-> PostalAddress.provinceCode
  )

  val tokenToSchemaMap = Map(
    POSTAL_CODE -> "postalCode",
    STREET_ADDRESS -> "streetAddress",
    CITY -> "addressLocality",
    PROVINCE -> "addressRegion",
    PROVINCE + ABBR -> "addressRegion"
  )



  implicit val formats = net.liftweb.json.DefaultFormats
   lazy val formatList = {
    val jsonStr = scala.io.Source.fromInputStream(
      getClass.getClassLoader.getResourceAsStream("addressformatter/libaddress-data-4sq.txt")
    ).getLines.toList.mkString("")
    val json = JsonParser.parse(jsonStr)
    json.extract[List[AddressFormat]]
  }
}

abstract class AddressFormatterMixin {
  def formatList: List[AddressFormat]
  lazy val formatMap = formatList.map(f => (f.id, f)).toMap

  def countryUsesStateAbbreviation(cc: String) = findFormat(cc).exists(_.fmt.exists(_.contains("PROVINCE+ABBR")))
  def countryUsesState(cc: String) = findFormat(cc).exists(_.fmt.exists(_.contains("PROVINCE")))
  def countryUsesCountyAsState(cc: String) = findFormat(cc).exists(_.state_name_type =? Some("county"))

  def findFormat(cc: String): Option[AddressFormat] = {
    formatMap.get(cc)
  }

  def findFormat(cc: String, subKey: String): Option[AddressFormat] = {
    formatMap.get(cc + "/" + subKey)
  }

  def findFormatByPostalCode(cc: String, postalCode: String): Option[AddressFormat] = {
    (for {
      ccFormat <- findFormat(cc).view
      subKey <- ccFormat.sub_keys.view
      subFormat <- findFormat(cc, subKey)
      if subFormat.zipMatches(postalCode)
    } yield {
      subFormat
    }).headOption
  }

  def doFormat(
    p: PostalAddress,
    options: AddressFormatterOptions = AddressFormatterOptions()
  ): FormattedAddress = {
    val cc = p.countryCodeOption.getOrElse("US")
    val format = findFormat(cc).getOrElse(findFormat("US").get)
    format.doFormat(p, options)
  }

  def reformatZip(cc: String, z: String): String = {
    findFormat(cc).map(_.reformatZip(z)).getOrElse(z)
  }

  def isZipValid(cc: String, z: String): Boolean = {
    findFormat(cc).map(_.zipMatches(z)).getOrElse(true)
  }
}
