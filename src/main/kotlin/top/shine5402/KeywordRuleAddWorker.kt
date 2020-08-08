package top.shine5402

import java.lang.RuntimeException

abstract class KeywordRuleAddWorker(val keywordRules: MutableList<KeywordRule>) {
    /**
     * @return 给用户的回执信息
     * */

    protected abstract fun modifyReply(rule: KeywordRule, newReplies: List<String>): String?

    fun add(type: String, keyword: String, replies: MutableList<String>, matchGroupID: List<Long>): String {
        KeywordReply.findRulesByKeyword(keyword).filter {
            it.groups.sorted() == matchGroupID.sorted()
        }.map {
            if (it.type != type)
                throw RuleTypeConflictException(it)
            return (modifyReply(it, replies) ?: doAdd(type, keyword, replies, matchGroupID))
        }

        return doAdd(type, keyword, replies, matchGroupID)
    }

    private fun doAdd(
        type: String,
        keyword: String,
        replies: MutableList<String>,
        matchGroupID: List<Long>
    ): String {
        KeywordReply.keywordRules.add(KeywordRuleFactory.create(type, keyword, replies, matchGroupID))
        return "关键字添加成功。\n" + KeywordReply.keywordRules.last().toStringHumanFriendly()
    }

}

class RuleTypeConflictException(val existRule: KeywordRule) :
    RuntimeException("There has been a same keyword rule with different type.")

class AddKeywordRuleAddWorker(keywordRules: MutableList<KeywordRule>) : KeywordRuleAddWorker(keywordRules) {
    override fun modifyReply(rule: KeywordRule, newReplies: List<String>): String? {
        return null
    }
}

class MergeKeywordRuleAddWorker(keywordRules: MutableList<KeywordRule>) : KeywordRuleAddWorker(keywordRules) {
    override fun modifyReply(rule: KeywordRule, newReplies: List<String>): String? {
        rule.replies.addAll(newReplies)
        return "关键字规则已经和之前相同关键字的合并。\n" + rule.toStringHumanFriendly()
    }
}

class CoverKeywordRuleAddWorker(keywordRules: MutableList<KeywordRule>) : KeywordRuleAddWorker(keywordRules) {
    override fun modifyReply(rule: KeywordRule, newReplies: List<String>): String? {
        rule.replies.clear()
        rule.replies.addAll(newReplies)
        return "新的关键词规则已经覆盖旧的、有相同关键词的规则。\n" + rule.toStringHumanFriendly()
    }
}

class KeepKeywordRuleAddWorker(keywordRules: MutableList<KeywordRule>) : KeywordRuleAddWorker(keywordRules) {
    override fun modifyReply(rule: KeywordRule, newReplies: List<String>): String? {
        return "该关键词已存在，规则没有被修改。\n" + rule.toStringHumanFriendly()
    }
}