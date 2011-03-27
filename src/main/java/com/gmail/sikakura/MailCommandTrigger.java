package com.gmail.sikakura;

import static hudson.Util.*;
import hudson.*;
import hudson.cli.*;
import hudson.console.*;
import hudson.model.*;
import hudson.scheduler.*;
import hudson.triggers.*;
import hudson.util.*;

import java.io.*;
import java.nio.charset.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;

import javax.mail.*;

import org.apache.commons.jelly.*;
import org.dom4j.*;
import org.dom4j.io.*;
import org.kohsuke.stapler.*;

import antlr.*;

/**
 * @author Naoto Shikakura
 */
public class MailCommandTrigger extends Trigger<SCMedItem> {

    private static final Logger LOGGER = Logger.getLogger(MailCommandTrigger.class.getName());

    @DataBoundConstructor
    public MailCommandTrigger(String spec) throws ANTLRException {
        super(spec);
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        return Collections.singleton(new MailCommandAction());
    }

    public File getLogFile() {
        return new File(job.getRootDir(), "mailcommander-polling.log");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {

        String address = null, port = null, username = null, password = null;
        boolean isCommandExist = false;
        try {
            FileInputStream fis = new FileInputStream(new File(job.getRootDir(), "config.xml"));
            Document dom = new SAXReader().read(fis);

            Element mailcommander =
                dom.getRootElement().element("builders").element(MailCommandBuilder.class.getName());
            address = mailcommander.elementText("address");
            port = mailcommander.elementText("port");
            username = mailcommander.elementText("username");
            password = mailcommander.elementText("password");
            fis.close();
        } catch (IOException ioe) {
            // TODO Auto-generated catch block
            ioe.printStackTrace();

        } catch (DocumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Properties props = new Properties();
        Session sess = Session.getDefaultInstance(props);
        Store store;
        try {
            store = sess.getStore("pop3");
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
            return;
        }

        StringBuffer commands = new StringBuffer();
        for (CLICommand c : CLICommand.all())
            commands.append(c.getName());

        try {
            store.connect(address, Integer.valueOf(port), username, password);

            Folder rootFolder = store.getDefaultFolder();
            Folder inbox = rootFolder.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            Message[] messages = null;
            if (inbox.getMessageCount() > 10)
                messages = inbox.getMessages(inbox.getMessageCount() - 10 + 1, inbox.getMessageCount());
            else
                messages = inbox.getMessages();
            FetchProfile fp = new FetchProfile();
            fp.add("Subject");
            inbox.fetch(messages, fp);
            for (Message message : messages) {
                String findStr = message.getSubject().split(" ")[0];
                if (commands.indexOf(findStr) != -1) {
                    isCommandExist = true;
                    break;
                }
            }
            inbox.close(true);
            store.close();
        } catch (MessagingException e) {
            e.printStackTrace();
            return;
        }

        if (isCommandExist)
            job.scheduleBuild(0, new MailCommandTriggerCause());

        try {
            StreamTaskListener listener = new StreamTaskListener(getLogFile());

            try {
                PrintStream logger = listener.getLogger();
                logger.println("Started on " + DateFormat.getDateTimeInstance().format(new Date()));
                if (isCommandExist)
                    logger.println("Changes found");
                else
                    logger.println("No changes");
            } catch (Error e) {
                e.printStackTrace(listener.error("Failed to record Mail Commander polling"));
                LOGGER.log(Level.SEVERE, "Failed to record SCM polling", e);
                throw e;
            } catch (RuntimeException e) {
                e.printStackTrace(listener.error("Failed to record Mail Commander polling"));
                LOGGER.log(Level.SEVERE, "Failed to record Mail Commander polling", e);
                throw e;
            } finally {
                listener.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to record Mail Commander polling", e);
        }

    }

    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {
        public boolean isApplicable(Item item) {
            return item instanceof BuildableItem;
        }

        public String getDisplayName() {
            return Messages.MailCommandTrigger_DisplayName();
        }

        // backward compatibility
        public FormValidation doCheck(@QueryParameter String value) {
            return doCheckSpec(value);
        }

        /**
         * Performs syntax check.
         */
        public FormValidation doCheckSpec(@QueryParameter String value) {
            try {
                String msg = CronTabList.create(fixNull(value)).checkSanity();
                if (msg != null)
                    return FormValidation.warning(msg);
                return FormValidation.ok();
            } catch (ANTLRException e) {
                return FormValidation.error(e.getMessage());
            }
        }
    }

    public static class MailCommandTriggerCause extends Cause {
        @Override
        public String getShortDescription() {
            return "Mail Commander Trigger";
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof MailCommandTriggerCause;
        }

        @Override
        public int hashCode() {
            return 5;
        }
    }

    public final class MailCommandAction implements Action {

        public AbstractProject<?, ?> getOwner() {
            return job.asProject();
        }

        public String getIconFileName() {
            return "clipboard.gif";
        }

        public String getDisplayName() {
            return "Mail Command Action";
        }

        public String getUrlName() {
            return "mailcommandPollLog";
        }

        public String getLog() throws IOException {
            return Util.loadFile(getLogFile());
        }

        /**
         * Writes the annotated log to the given output.
         * 
         * @since 1.350
         */
        public void writeLogTo(XMLOutput out) throws IOException {
            new AnnotatedLargeText<MailCommandAction>(getLogFile(), Charset.defaultCharset(), true, this).writeHtmlTo(
                0,
                out.asWriter());
        }
    }

}
