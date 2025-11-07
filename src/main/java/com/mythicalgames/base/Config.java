package com.mythicalgames.base;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;

@Header({
    "###############################################",
    "# Thank you for downloading AllayBasePlugin",
    "# You are now part of our Mythical Ecosystem",
    "###############################################"
})

public class Config extends OkaeriConfig {
    @Comment(" ")
    @Comment(" ")

    @Comment({
        "Love is not fake, Peoples are!",
        "Fuck love!"
    })
    public boolean heartbroken = true;
}