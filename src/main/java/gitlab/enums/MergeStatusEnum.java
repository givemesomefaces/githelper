package gitlab.enums;

import lombok.Getter;

/**
 * @author Lv LiFeng
 * @date 2022/1/15 10:53
 */
@Getter
public enum MergeStatusEnum {
    CAN_BE_MERGED("can_be_merged"),
    CANNOT_BE_MERGED("cannot_be_merged"),
    MERGED("merged"),
    CLOSED("closed"),

    ;

    private String mergeStatus;

    MergeStatusEnum(String mergeStatus) {
        this.mergeStatus = mergeStatus;
    }
}
