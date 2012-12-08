package jake2.qcommon;

import jake2.Jake2;

import java.util.Locale;

/** @author Patrick Woodworth */
public class Q2TestDataUtil {

    private static final boolean USE_DATA_DIALOG = Boolean.getBoolean("jake.data.dialog");

    public static final void initQ2DataTool() {
        if (USE_DATA_DIALOG) {
            Q2DataDialogWrapper tmpq2DataTool = new Q2DataDialogWrapper();
            Locale.setDefault(Locale.US);
            tmpq2DataTool.setVisible(true);
            Jake2.q2DataTool = tmpq2DataTool;
        } else {
            Jake2.q2DataTool = new Q2DataTool();
            Locale.setDefault(Locale.US);
        }
    }

    public static class Q2DataDialogWrapper extends Q2DataTool {

        private final Q2DataDialog m_data = new Q2DataDialog();

        public Q2DataDialogWrapper() {
            super();
        }

        @Override
        public void testQ2Data() {
            m_data.testQ2Data();
        }

        @Override
        void dispose() {
            m_data.dispose();
        }

        @Override
        void setStatus(String text) {
            m_data.setStatus(text);
        }

        public void setVisible(boolean b) {
            m_data.setVisible(b);
        }
    }
}
