package top.shine5402

import java.lang.RuntimeException

interface KeywordRuleModifyWorker {
    fun modify(rules: MutableList<KeywordRule>, id : Int, param: String, value: String, separator: String = "*"){
        val oldRule = rules[id]
        when (param){
            "type" -> KeywordRuleFactory.create(value, oldRule.keyword, oldRule.replies, oldRule.groups)
            "keyword" -> KeywordRuleFactory.create(oldRule.type, value, oldRule.replies, oldRule.groups)
            else -> null
        }?.let{
            rules[id] = it
        }
        when (param){
            "replies" -> KeywordRuleFactory.create(oldRule.type, oldRule.keyword, value.split(separator).toMutableList(), oldRule.groups)
            "groups" -> KeywordRuleFactory.create(oldRule.type, oldRule.keyword, oldRule.replies, value.split(separator)
                .map{it.toLongOrNull() ?: 0}.filter { it != 0L }.toMutableList())
            else -> null
        }?.let {
            rules[id] = it
        }
    }
}

object KeywordRuleModifyWorkerFactory{
    fun get(type: String): KeywordRuleModifyWorker{
        return when (type){
            "cover" -> CoverKeywordRuleModifyWorker
            "append" -> AppendKeywordRuleModifyWorker
            "remove" -> RemoveKeywordRuleModifyWorker
            "clear" -> ClearKeywordRuleModifyWorker
            else -> throw ModifyWorkerTypeNotExist(type)
        }
    }
    class ModifyWorkerTypeNotExist(wrongType: String) : RuntimeException("There is not a ModifyWorker type called $wrongType")
}

object CoverKeywordRuleModifyWorker : KeywordRuleModifyWorker{
}

object AppendKeywordRuleModifyWorker : KeywordRuleModifyWorker{
    override fun modify(rules: MutableList<KeywordRule>, id: Int, param: String, value: String, separator: String) {
        val oldRule = rules[id]
        val newRule = when (param){
            "replies" -> KeywordRuleFactory.create(oldRule.type, oldRule.keyword, listOf(oldRule.replies, value.split(separator)).flatten()
                .toMutableList(), oldRule.groups)
            "groups" -> KeywordRuleFactory.create(oldRule.type, oldRule.keyword, oldRule.replies, listOf(oldRule.groups, value.split(separator)
                .map{it.toLongOrNull() ?: 0}.filter { it != 0L }).flatten().toMutableList())
            else -> null
        }
        if (newRule != null){
            rules[id] = newRule
        }
        else
        {
            super.modify(rules, id, param, value, separator)
        }
    }
}

object RemoveKeywordRuleModifyWorker : KeywordRuleModifyWorker{
    override fun modify(rules: MutableList<KeywordRule>, id: Int, param: String, value: String, separator: String) {
        val oldRule = rules[id]
        val newRule = when (param){
            "replies" -> KeywordRuleFactory.create(oldRule.type, oldRule.keyword,
                oldRule.replies.filter { !value.split(separator).contains(it) }.toMutableList(), oldRule.groups)
            "groups" -> KeywordRuleFactory.create(oldRule.type, oldRule.keyword, oldRule.replies,
                oldRule.groups.filter { !value.split(separator).map{it.toLongOrNull() ?: 0}.filter { it != 0L }.contains(it) })
            else -> null
        }
        if (newRule != null){
            rules[id] = newRule
        }
        else
        {
            super.modify(rules, id, param, value, separator)
        }
    }
}

object ClearKeywordRuleModifyWorker : KeywordRuleModifyWorker{
    override fun modify(rules: MutableList<KeywordRule>, id: Int, param: String, value: String, separator: String) {
        val oldRule = rules[id]
        val newRule = when (param){
            "groups" -> KeywordRuleFactory.create(oldRule.type, oldRule.keyword, oldRule.replies, listOf())
            else -> null
        }
        if (newRule != null){
            rules[id] = newRule
        }
        else
        {
            super.modify(rules, id, param, value, separator)
        }
    }
}