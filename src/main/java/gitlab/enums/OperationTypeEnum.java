package gitlab.enums;

import lombok.Getter;

/**
 *
 *
 * @author Lv LiFeng
 * @date 2022/1/12 15:23
 */
@Getter
public enum OperationTypeEnum {
    CREATE_MERGE_REQUEST("create", "Create merge request result"),
    CLOSE_MERGE_REQUEST("close", "Close merge request result"),
    MERGE("merge", "Merge request result")
    ;


    private String type;
    private String dialogTitle;

    OperationTypeEnum(String type, String dialogTitle) {
        this.type = type;
        this.dialogTitle = dialogTitle;
    }
}
