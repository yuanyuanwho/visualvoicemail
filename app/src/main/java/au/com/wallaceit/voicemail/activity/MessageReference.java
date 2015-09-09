package au.com.wallaceit.voicemail.activity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.VisualVoicemail;
import com.fsck.k9.Preferences;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mail.filter.Base64;

import java.util.StringTokenizer;

public class MessageReference implements Parcelable {
    private final String accountUuid;
    private final String folderName;
    private final String uid;
    private final Flag flag;


    /**
     * Initialize a new MessageReference.
     */
    public MessageReference(String accountUuid, String folderName, String uid, Flag flag) {
        this.accountUuid = accountUuid;
        this.folderName = folderName;
        this.uid = uid;
        this.flag = flag;
    }

    // Version identifier for use when serializing. This will allow us to introduce future versions
    // if we have to rev MessageReference.
    private static final String IDENTITY_VERSION_1 = "!";
    private static final String IDENTITY_SEPARATOR = ":";

    /**
     * Initialize a MessageReference from a serialized identity.
     * @param identity Serialized identity.
     * @throws MessagingException On missing or corrupted identity.
     */
    public MessageReference(final String identity) throws MessagingException {
        // Can't be null and must be at least length one so we can check the version.
        if (identity == null || identity.length() < 1) {
            throw new MessagingException("Null or truncated MessageReference identity.");
        }

        String accountUuid = null;
        String folderName = null;
        String uid = null;
        Flag flag = null;
        // Version check.
        if (identity.charAt(0) == IDENTITY_VERSION_1.charAt(0)) {
            // Split the identity, stripping away the first two characters representing the version and delimiter.
            StringTokenizer tokens = new StringTokenizer(identity.substring(2), IDENTITY_SEPARATOR, false);
            if (tokens.countTokens() >= 3) {
                accountUuid = Base64.decode(tokens.nextToken());
                folderName = Base64.decode(tokens.nextToken());
                uid = Base64.decode(tokens.nextToken());

                if (tokens.hasMoreTokens()) {
                    final String flagString = tokens.nextToken();
                    try {
                        flag = Flag.valueOf(flagString);
                    } catch (IllegalArgumentException ie) {
                        throw new MessagingException("Could not thaw message flag '" + flagString + "'", ie);
                    }
                }

                if (VisualVoicemail.DEBUG) {
                    Log.d(VisualVoicemail.LOG_TAG, "Thawed " + toString());
                }
            } else {
                throw new MessagingException("Invalid MessageReference in " + identity + " identity.");
            }
        }
        this.accountUuid = accountUuid;
        this.folderName = folderName;
        this.uid = uid;
        this.flag = flag;
    }

    /**
     * Serialize this MessageReference for storing in a VisualVoicemail identity.  This is a colon-delimited base64 string.
     *
     * @return Serialized string.
     */
    public String toIdentityString() {
        StringBuilder refString = new StringBuilder();

        refString.append(IDENTITY_VERSION_1);
        refString.append(IDENTITY_SEPARATOR);
        refString.append(Base64.encode(accountUuid));
        refString.append(IDENTITY_SEPARATOR);
        refString.append(Base64.encode(folderName));
        refString.append(IDENTITY_SEPARATOR);
        refString.append(Base64.encode(uid));
        if (flag != null) {
            refString.append(IDENTITY_SEPARATOR);
            refString.append(flag.name());
        }

        return refString.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof com.fsck.k9.activity.MessageReference == false) {
            return false;
        }
        com.fsck.k9.activity.MessageReference other = (com.fsck.k9.activity.MessageReference) o;
        if ((accountUuid == other.accountUuid || (accountUuid != null && accountUuid.equals(other.accountUuid)))
                && (folderName == other.folderName || (folderName != null && folderName.equals(other.folderName)))
                && (uid == other.uid || (uid != null && uid.equals(other.uid)))) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int MULTIPLIER = 31;

        int result = 1;
        result = MULTIPLIER * result + ((accountUuid == null) ? 0 : accountUuid.hashCode());
        result = MULTIPLIER * result + ((folderName == null) ? 0 : folderName.hashCode());
        result = MULTIPLIER * result + ((uid == null) ? 0 : uid.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "MessageReference{" +
               "accountUuid='" + accountUuid + '\'' +
               ", folderName='" + folderName + '\'' +
               ", uid='" + uid + '\'' +
               ", flag=" + flag +
               '}';
    }

    public LocalMessage restoreToLocalMessage(Context context) {
        try {
            Account account = Preferences.getPreferences(context).getAccount(accountUuid);
            if (account != null) {
                LocalFolder folder = account.getLocalStore().getFolder(folderName);
                if (folder != null) {
                    LocalMessage message = folder.getMessage(uid);
                    if (message != null) {
                        return message;
                    } else {
                        Log.d(VisualVoicemail.LOG_TAG, "Could not restore message, uid " + uid + " is unknown.");
                    }
                } else {
                    Log.d(VisualVoicemail.LOG_TAG, "Could not restore message, folder " + folderName + " is unknown.");
                }
            } else {
                Log.d(VisualVoicemail.LOG_TAG, "Could not restore message, account " + accountUuid + " is unknown.");
            }
        } catch (MessagingException e) {
            Log.w(VisualVoicemail.LOG_TAG, "Could not retrieve message for reference.", e);
        }

        return null;
    }

    public static final Creator<com.fsck.k9.activity.MessageReference> CREATOR = new Creator<com.fsck.k9.activity.MessageReference>() {
        @Override
        public com.fsck.k9.activity.MessageReference createFromParcel(Parcel source) {
            com.fsck.k9.activity.MessageReference ref;
            String uid = source.readString();
            String accountUuid = source.readString();
            String folderName = source.readString();
            String flag = source.readString();
            if (flag != null) {
                ref = new com.fsck.k9.activity.MessageReference(accountUuid, folderName, uid, Flag.valueOf(flag));
            } else {
                ref = new com.fsck.k9.activity.MessageReference(accountUuid, folderName, uid, null);
            }
            return ref;
        }

        @Override
        public com.fsck.k9.activity.MessageReference[] newArray(int size) {
            return new com.fsck.k9.activity.MessageReference[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uid);
        dest.writeString(accountUuid);
        dest.writeString(folderName);
        dest.writeString(flag == null ? null : flag.name());
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public String getFolderName() {
        return folderName;
    }

    public String getUid() {
        return uid;
    }

    public Flag getFlag() {
        return flag;
    }

    public com.fsck.k9.activity.MessageReference withModifiedUid(String newUid) {
        return new com.fsck.k9.activity.MessageReference(accountUuid, folderName, newUid, flag);
    }

    public com.fsck.k9.activity.MessageReference withModifiedFlag(Flag newFlag) {
        return new com.fsck.k9.activity.MessageReference(accountUuid, folderName, uid, newFlag);
    }
}
