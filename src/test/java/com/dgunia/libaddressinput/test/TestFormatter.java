package com.dgunia.libaddressinput.test;

import com.android.i18n.addressinput.AddressData;
import com.android.i18n.addressinput.FormOptions;
import com.android.i18n.addressinput.FormatInterpreter;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class TestFormatter {
    @Test
    public void testDE() throws Exception {
        List<String> result = new FormatInterpreter(new FormOptions.Builder().build()).getEnvelopeAddress(new AddressData.Builder()
                .setPostalCode("3000")
                .setCountry("DE")
                .setLocality("Hannover")
                .setRecipient("Max Mustermann")
                .setAddress("Hildesheimer Str. 1")
                .build());

        Assert.assertEquals(3, result.size());
        Assert.assertEquals("Max Mustermann", result.get(0));
        Assert.assertEquals("Hildesheimer Str. 1", result.get(1));
        Assert.assertEquals("3000 Hannover", result.get(2));
    }

    @Test
    public void testUS() throws Exception {
        List<String> result = new FormatInterpreter(new FormOptions.Builder().build()).getEnvelopeAddress(new AddressData.Builder()
                .setPostalCode("3000")
                .setCountry("US")
                .setLocality("Hannover")
                .setRecipient("Max Mustermann")
                .setAddress("Hildesheimer Str. 1")
                .build());

        Assert.assertEquals(3, result.size());
        Assert.assertEquals("Max Mustermann", result.get(0));
        Assert.assertEquals("Hildesheimer Str. 1", result.get(1));
        Assert.assertEquals("Hannover 3000", result.get(2));
    }
}
