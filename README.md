libaddressinput
===============

fork of google's libaddressinput with the android bits ripped out and the address formatter exposed


import com.android.i18n.addressinput._

val formatInterpreter = new FormatInterpreter(new FormOptions.Builder().build());

val    US_CA_ADDRESS = (new AddressData.Builder().setCountry("US")
                                                 .setAdminArea("CA")
                                                 .setLocality("Mt View")
                                                 .setAddressLine1("1098 Alta Ave")
                                                 .setPostalCode("94043")
                                                 .build())
formatInterpreter.getEnvelopeAddress(US_CA_ADDRESS)
// res0: java.util.List[String] = [1098 Alta Ave, Mt View CA 94043]

       val TW_ADDRESS = (new AddressData.Builder().setCountry("TW")
                                              .setAdminArea("\u53F0\u5317\u5E02")  // Taipei city
                                              .setLocality("\u5927\u5B89\u5340")  // Da-an district
                                              .setAddressLine1("Sec. 3 Hsin-yi Rd.")
                                              .setPostalCode("106")
                                              .setOrganization("Giant Bike Store")
                                              .setRecipient("Mr. Liu")
                                              .build())
formatInterpreter.getEnvelopeAddress(TW_ADDRESS)
// res1: java.util.List[String] = [106, 台北市大安區, Sec. 3 Hsin-yi Rd., Giant Bike Store, Mr. Liu]

// the zh-Latn here is a weird and awful hack
       val TW_ADDRESS2 = (new AddressData.Builder().setCountry("TW")
                                              .setAdminArea("\u53F0\u5317\u5E02")  // Taipei city
                                              .setLocality("\u5927\u5B89\u5340")  // Da-an district
                                              .setAddressLine1("Sec. 3 Hsin-yi Rd.")
                                              .setPostalCode("106")
                                              .setOrganization("Giant Bike Store")
                                              .setRecipient("Mr. Liu")
                                              .setLanguageCode("zh-Latn")
                                              .build())
formatInterpreter.getEnvelopeAddress(TW_ADDRESS2)
// res5: java.util.List[String] = [Mr. Liu, Giant Bike Store, Sec. 3 Hsin-yi Rd., 大安區, 台北市 106]
