package com.actelion.research.table.view.card.positioning;

public interface SortingConfigurationListener {

    public void sortingConfigurationChanged(SortingConfigurationListener.SortingCofigurationEvent e);


    public static class SortingCofigurationEvent{
        private CardPositionerWithGUI mSource;
        private String  mMessage;
        private boolean mForceSorting;

        public SortingCofigurationEvent(CardPositionerWithGUI source, String message, boolean forceSorting){
            setMessage(message);
            this.setForceSorting(forceSorting);
            this.setSource(source);
        }

        public String getMessage() {
            return mMessage;
        }

        public void setMessage(String mMessage) {
            this.mMessage = mMessage;
        }

        public boolean isForceSorting() {
            return mForceSorting;
        }

        public void setForceSorting(boolean mForceSorting) {
            this.mForceSorting = mForceSorting;
        }

        public CardPositionerWithGUI getSource() {
            return mSource;
        }

        public void setSource(CardPositionerWithGUI source) {
            this.mSource = source;
        }
    }
}
