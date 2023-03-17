package com.github.lvlifeng.githelper.bean

import lombok.Getter
import lombok.Setter
import lombok.experimental.Accessors
import org.apache.commons.lang3.BooleanUtils
import java.util.*

/**
 * @author Lv LiFeng
 * @date 2022/1/7 00:18
 */
@Getter
@Setter
@Accessors(chain = true)
class GitlabServer {
    var apiUrl = ""
    var apiToken = ""
    var repositoryUrl = ""
    var preferredConnection = CloneType.SSH
    var validFlag: Boolean? = null
    override fun toString(): String {
        if (Objects.equals(false, validFlag)) {
            return "(invalid) $apiUrl"
        }
        return apiUrl
    }

    enum class CloneType {
        SSH, HTTPS
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }
        val that = o as GitlabServer
        return apiUrl == that.apiUrl && apiToken == that.apiToken && repositoryUrl == that.repositoryUrl && preferredConnection == that.preferredConnection
    }

    override fun hashCode(): Int {
        return Objects.hash(apiUrl, apiToken, repositoryUrl, preferredConnection)
    }
}