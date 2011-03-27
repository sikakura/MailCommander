package com.gmail.sikakura;

import org.jvnet.localizer.*;

@SuppressWarnings( { "", "PMD" })
public class Messages {

    private final static ResourceBundleHolder holder = ResourceBundleHolder.get(Messages.class);

    public static String MailCommandPublisher_DisplayName() {
        return holder.format("MailCommandPublisher.DisplayName");
    }

    public static Localizable _MailCommandPublisher_DisplayName() {
        return new Localizable(holder, "MailCommandPublisher.DisplayName");
    }

    public static String MailCommandTrigger_DisplayName() {
        return holder.format("MailCommandTrigger.DisplayName");
    }

    public static Localizable _MailCommandTrigger_DisplayName() {
        return new Localizable(holder, "MailCommandTrigger.DisplayName");
    }

}
