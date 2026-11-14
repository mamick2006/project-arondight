package arondight.form;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fs.starfarer.api.fleet.FleetMemberAPI;

import arondight.ArondightIds;

public final class ArondightTransformer {

    private ArondightTransformer() {}

    public static List<String> getAllForms() {
        return ArondightIds.ALL_FORMS;
    }

    public static String getParentForm(String formId) {
        return FormTree.parentOf(formId);
    }

    public static int getTierOf(String formId) {
        return FormTree.tierOf(formId);
    }

    public static String getFormDisplayName(String formId) {
        return FormTree.displayName(formId);
    }

    public static String getTransformBlockReason(FleetMemberAPI member, String targetFormId) {
        return FormActions.getTransformBlockReason(member, targetFormId);
    }

    public enum FormSelectState {
        CURRENT,
        SELECT_OK,
        LOCKED_SEALED,
        LOCKED_DESCENT,
        LOCKED_CONTEXT,
        LOCKED_ECONOMY,
        NO_FRAME,
        UNKNOWN
    }

    public static FormSelectState getFormSelectState(FleetMemberAPI member, String formId) {
        return FormActions.getSelectState(member, formId);
    }

    public static String getSelectBlockReason(FleetMemberAPI member, String formId) {
        return FormActions.getSelectBlockReason(member, formId);
    }

    public static TransformResult transform(FleetMemberAPI member, String targetFormId) {
        return FormActions.transform(member, targetFormId, false);
    }

    public static TransformResult transformUnchecked(FleetMemberAPI member, String targetFormId) {
        return FormActions.transform(member, targetFormId, true);
    }

    public static List<String> stripOpOverflowToCargo(FleetMemberAPI member) {
        return FormActions.stripOpOverflowToCargo(member);
    }

    public static FleetMemberAPI createFrameMember() {
        return FormActions.createFrameMember();
    }

    public static final class TransformResult {
        public final boolean success;
        public final String refusalReason;
        public final List<String> weaponsMovedToCargo;

        TransformResult(boolean success, String refusalReason, List<String> moved) {
            this.success = success;
            this.refusalReason = refusalReason;
            this.weaponsMovedToCargo = moved == null
                    ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(moved));
        }

        static TransformResult ok(List<String> moved) {
            return new TransformResult(true, null, moved);
        }

        static TransformResult refused(String reason) {
            return new TransformResult(false, reason, null);
        }
    }
}
