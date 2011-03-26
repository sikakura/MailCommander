package com.gmail.sikakura;

import hudson.*;
import hudson.model.*;
import hudson.tasks.*;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.mail.*;
import javax.mail.Message.*;
import javax.mail.internet.*;

import net.sf.json.*;

import org.kohsuke.stapler.*;

/**
 * TODO javadoc
 */
public class MailCommandPublisher extends Publisher {

    private static final Logger LOGGER =
        Logger.getLogger(MailCommandPublisher.class.getName());

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {

        File logfile = build.getLogFile();
        StringBuffer logbuf = new StringBuffer();
        try {
            FileReader in = new FileReader(logfile);
            BufferedReader br = new BufferedReader(in);
            String line;
            while ((line = br.readLine()) != null) {
                logbuf.append(line);
                logbuf.append("\n");
            }
            br.close();
            in.close();
        } catch (IOException e) {
            listener.getLogger().println(e);
        }

        File addressFile = new File(build.getRootDir(), "tmp.address");
        String to_address = null;
        if (addressFile.exists()) {
            try {
                FileReader in = new FileReader(addressFile);
                BufferedReader br = new BufferedReader(in);
                String line;
                while ((line = br.readLine()) != null) {
                    to_address = line;
                }
                br.close();
                in.close();
            } catch (IOException e) {
                listener.getLogger().println(e);
            }

            if (to_address != null) {
                try {
                    MimeMessage msg =
                        new MimeMessage(Mailer.descriptor().createSession());
                    msg.setRecipients(RecipientType.TO, to_address);
                    msg.setFrom(new InternetAddress("admin@jenkins.com"));
                    msg.setSubject("This is a result of mail command");
                    msg.setSentDate(new Date());
                    msg.setText(logbuf.toString());
                    Transport.send(msg);
                } catch (MessagingException mex) {
                    listener.getLogger().println(mex);
                    mex.printStackTrace();
                }
            }
        }
        return true;

    }

    /**
     * {@inheritDoc}
     */
    public BuildStepMonitor getRequiredMonitorService() {
        // TODO Auto-generated method stub
        return BuildStepMonitor.NONE;
    }

    public static DescriptorImpl DESCRIPTOR;

    public static DescriptorImpl descriptor() {
        return Hudson.getInstance().getDescriptorByType(
            MailCommandPublisher.DescriptorImpl.class);
    }

    @Extension
    public static final class DescriptorImpl extends
            BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            load();
            DESCRIPTOR = this;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Mail Commander";
        }

        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData) {
            MailCommandPublisher m = new MailCommandPublisher();
            return m;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

    }

}
