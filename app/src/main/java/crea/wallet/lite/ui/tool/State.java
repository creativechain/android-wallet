package crea.wallet.lite.ui.tool;

import crea.wallet.lite.R;

public enum State {
    PREPARED, SIGNING, SENDING, SENT, REJECTED, FAILED;

    private CharSequence explRes;

    public CharSequence getExplRes() {
        return explRes;
    }

    public void setExplRes(CharSequence explRes) {
        this.explRes = explRes;
    }

    public int toStringResource() {
        switch (this) {
            case PREPARED:
                return R.string.prepared_transaction;
            case SIGNING:
                return R.string.signing_transaction;
            case SENDING:
                return R.string.sending_transaction;
            case SENT:
                return R.string.transaction_sent;
            case FAILED:
                return R.string.transaction_failed;
            case REJECTED:
                return R.string.transaction_rejected;
            default:
                return 0;
        }
    }

    public int toColorResource() {
        switch (this) {
            case PREPARED:
                return R.color.prepared_transaction;
            case SIGNING:
                return R.color.signing_transaction;
            case SENDING:
                return R.color.sending_transaction;
            case SENT:
                return R.color.green;
            case FAILED:
                return R.color.red;
            case REJECTED:
                return R.color.yellow;
            default:
                return 0;
        }
    }
}