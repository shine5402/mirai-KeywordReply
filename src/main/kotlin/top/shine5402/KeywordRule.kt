package top.shine5402

import net.mamoe.mirai.console.plugins.ConfigSection
import net.mamoe.mirai.message.data.*
import java.lang.RuntimeException

abstract class KeywordRule(val keyword: String, val replies: MutableList<String>, val matchGroupID: List<Long>) {
    val reply
        get() = replies.shuffled().last()
    abstract val type: String
    val shouldMatchGroup
        get() = matchGroupID.isNotEmpty()

    fun match(message: String, source: MessageSource): Boolean {
        val shouldGroupMatchingTriggered = shouldMatchGroup && source.isAboutGroup()
        return (if (shouldGroupMatchingTriggered) groupMatching(source) else true)
                && keyMatching(message)
    }

    fun groupMatching(source: MessageSource): Boolean = source is OnlineMessageSource.Incoming.FromGroup && matchGroupID.contains(source.group.id)

    abstract fun keyMatching(message: String): Boolean

    fun toConfigSection(): ConfigSection
    {
        val section = ConfigSection.create()
        section["type"] = type
        section["keyword"] = keyword
        section["replies"] = replies
        section["matchGroupID"] = matchGroupID
        return section
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KeywordRule

        if (keyword != other.keyword) return false
        if (replies != other.replies) return false
        if (matchGroupID != other.matchGroupID) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = keyword.hashCode()
        result = 31 * result + replies.hashCode()
        result = 31 * result + matchGroupID.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    override fun toString(): String {
        return "KeywordRule(keyword='$keyword', reply='$replies', matchGroupID=$matchGroupID, type='$type')"
    }

    fun toStringHumanFriendly() : String{
        return "匹配模式：$type；" +
                "关键字：$keyword；" +
                "回复：$replies；" +
                "开启群：${if (matchGroupID.isEmpty()) "所有群" else matchGroupID.map { it.toString() }.joinToString(";")}。"
    }
}

object KeywordRuleFactory{
    class NotSupportedKeywordRuleType : RuntimeException("The given type of keyword rule is not supported"){}

    fun fromConfigSection(config: ConfigSection) : KeywordRule {
        val type = config.getString("type")
        val keyword = config.getString("keyword")
        val replies = config.getStringList("replies").toMutableList()
        val matchGroupID = config.getLongList("matchGroupID")
        return create(type, keyword, replies, matchGroupID)
    }

    fun create(type: String, keyword: String, replies: MutableList<String>, matchGroupID: List<Long>) : KeywordRule {
        return when (type){
            "exact" -> ExactKeywordRule(keyword, replies, matchGroupID)
            "vague" -> VagueKeywordRule(keyword, replies, matchGroupID)
            "regex" -> RegexKeywordRule(keyword, replies, matchGroupID)
            else -> throw NotSupportedKeywordRuleType()
        }
    }
}

class ExactKeywordRule(keyword: String, replies: MutableList<String>, matchGroupID: List<Long> = listOf()) : KeywordRule(keyword, replies, matchGroupID) {
    override val type: String
        get() = "exact"

    override fun keyMatching(message: String): Boolean {
        return message == keyword
    }
}

class VagueKeywordRule(keyword: String, replies: MutableList<String>, matchGroupID: List<Long>) : KeywordRule(keyword, replies, matchGroupID){
    override val type: String
        get() = "vague"

    override fun keyMatching(message: String): Boolean {
        return message.contains(keyword)
    }
}

class RegexKeywordRule(keyword: String, replies: MutableList<String>, matchGroupID: List<Long>) : KeywordRule(keyword, replies, matchGroupID){
    override val type: String
        get() = "regex"

    override fun keyMatching(message: String): Boolean {
        val regex = Regex(keyword)
        return regex.matches(message)
    }
}