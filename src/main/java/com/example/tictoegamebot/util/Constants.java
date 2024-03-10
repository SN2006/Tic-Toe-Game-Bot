package com.example.tictoegamebot.util;

import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Constants {

    public static final List<String> xSymbol = Arrays.asList("❌", "✖️");
    public static final List<String> oSymbol = Arrays.asList("⭕", "\uD83D\uDD34");
    public static final Random RANDOM = new Random();
    public static final InputFile menuPicture = new InputFile(new File("src/main/resources/img/MenuPicture.jfif"));
    public static final List<String> digits = Arrays.asList("0️⃣", "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣",
            "7️⃣", "8️⃣", "9️⃣");

}
