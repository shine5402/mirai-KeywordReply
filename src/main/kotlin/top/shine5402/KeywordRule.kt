package top.shine5402

import net.mamoe.mirai.console.plugins.ConfigSection
import net.mamoe.mirai.message.data.*
import java.lang.RuntimeException

abstract class KeywordRule(val keyword: String, val reply: String, val matchGroupID: List<Long>) {
    abstract val type: String
    val shouldMatchGroup
        get() = matchGroupID.isNotEmpty()

    fun match(message: String, source: MessageSource): Boolean {
        val shouldGroupMatchingTriggered = shouldMatchGroup && source.isAboutGroup()
        return (if (shouldGroupMatchingTriggered) groupMatching(source) else true)
                && keyMatching(message)
    }

    fun groupMatching(source: MessageSource): Boolean = source.isAboutGroup() && matchGroupID.contains(source.fromId)

    abstract fun keyMatching(message: String): Boolean

    fun toConfigSection(): ConfigSection
    {
        val section = ConfigSection.create()
        section["type"] = type
        section["keyword"] = keyword
        section["reply"] = reply
        section["matchGroupID"] = matchGroupID
        return section
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KeywordRule

        if (keyword != other.keyword) return false
        if (reply != other.reply) return false
        if (matchGroupID != other.matchGroupID) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = keyword.hashCode()
        result = 31 * result + reply.hashCode()
        result = 31 * result + matchGroupID.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    override fun toString(): String {
        return "KeywordRule(keyword='$keyword', reply='$reply', matchGroupID=$matchGroupID, type='$type')"
    }

    fun toStringHumanFriendly() : String{
        return "匹配模式：$type；" +
                "关键字：$keyword；" +
                "回复：$reply；" +
                "开启群：${if (matchGroupID.isEmpty()) "所有群" else matchGroupID.map { it.toString() }.joinToString(";")}。"
    }
}

object KeywordRuleFactory{
    class NotSupportedKeywordRuleType : RuntimeException("The given type of keyword rule is not supported"){}

    fun fromConfigSection(config: ConfigSection) : KeywordRule {
        val type = config.getString("type")
        val keyword = config.getString("keyword")
        val reply = config.getString("reply")
        val matchGroupID = config.getLongList("matchGroupID")
        return create(type, keyword, reply, matchGroupID)
    }

    fun create(type: String, keyword: String, reply: String, matchGroupID: List<Long>) : KeywordRule {
        return when (type){
            "exact" -> ExactKeywordRule(keyword, reply, matchGroupID)
            "vague" -> VagueKeywordRule(keyword, reply, matchGroupID)
            "regex" -> RegexKeywordRule(keyword, reply, matchGroupID)
            else -> throw NotSupportedKeywordRuleType()
        }
    }
}

class ExactKeywordRule(keyword: String, reply: String, matchGroupID: List<Long> = listOf()) : KeywordRule(keyword, reply, matchGroupID) {
    override val type: String
        get() = "exact"

    override fun keyMatching(message: String): Boolean {
        return message.toString() == keyword
    }
}

class VagueKeywordRule(keyword: String, reply: String, matchGroupID: List<Long>) : KeywordRule(keyword, reply, matchGroupID){
    override val type: String
        get() = "vague"

    override fun keyMatching(message: String): Boolean {
        return message.toString().contains(keyword)
    }
}

class RegexKeywordRule(keyword: String, reply: String, matchGroupID: List<Long>) : KeywordRule(keyword, reply, matchGroupID){
    override val type: String
        get() = "regex"

    override fun keyMatching(message: String): Boolean {
        val regex = Regex(keyword)
        return regex.matches(message)
    }
}