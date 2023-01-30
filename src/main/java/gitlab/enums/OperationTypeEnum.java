package gitlab.enums;

import lombok.Getter;

/**
 * @author Lv LiFeng
 * @date 2022/1/12 15:23
 */
@Getter
public enum OperationTypeEnum {
    CREATE_MERGE_REQUEST("create", "Results Of Creating Merge Request"),
    CLOSE_MERGE_REQUEST("close", "Results Of Closing Merge Request"),
    MERGE("merge", "Results Of Merging Request"),
    CREATE_TAG("tag", "Results Of Creating Tag"),
    ;


    private String type;
    private String dialogTitle;

    OperationTypeEnum(String type, String dialogTitle) {
        this.type = type;
        this.dialogTitle = dialogTitle;
    }
}
