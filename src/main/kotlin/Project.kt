package calculator

import java.lang.Exception
import java.math.BigInteger
import kotlin.system.exitProcess

val localMemory = mutableMapOf<String, BigInteger>()
val commandList = listOf("/help", "/exit")

fun main() {
    while (true) {
        val commandRegex = "/[a-zA-Z0-9]*".toRegex()

        val memoryRegex = "\\s*[a-zA-Z]+\\s*=\\s*-?\\s*\\d+\\s*".toRegex()
        val inMemoryRegex = "\\s*[a-zA-Z]+\\s*=\\s*[a-zA-Z]+\\s*".toRegex()
        val rowInput = readln()
        val args: Array<String> = rowInput.trim().replace("\\s+".toRegex(), " ").split(" ").toTypedArray()
        when {
            commandRegex.matches(args.first()) && args.first() == "/exit" -> {
                println("Bye!")
                exitProcess(0)
            }

            commandRegex.matches(args.first()) && args.first() == "/help" -> println("The program calculates the sum of numbers")
            commandRegex.matches(args.first()) && !commandList.contains(args.first()) -> println("Unknown command")
            args.isEmpty() -> continue
            args.size == 1 && args.first().isEmpty() -> continue
            args.size == 1 && "d+".toRegex().matches(args.first()) -> println(args.first().replace("\\+".toRegex(), ""))
            args.size == 1 && "[a-zA-Z]+".toRegex().matches(args.first()) -> findElementInLocalMemory(args.first())
            memoryRegex.matches(rowInput) -> executeSaveInLocalMemory(rowInput)
            inMemoryRegex.matches(rowInput) -> executeSaveInLocalMemoryWithVariable(rowInput)
            isACorrectExpression(rowInput) -> executeChainedOperationWithVariables(args)?.let { println(it) }
            validateParenthesesFormat(rowInput) -> executeChainedOperationWithParentheses(rowInput)
            else -> println("Invalid identifier")
        }
    }
}

fun isACorrectExpression(expression: String): Boolean {
    val expressionVariablesRegex = "[-+]?((\\s*[a-zA-Z]+|[0-9]+)\\s*([*/+-]+\\s*([a-zA-Z]+|[0-9]+)\\s*)*)".toRegex()
    val nonMultiplicationRegex = ".*[*]{2,}.*".toRegex()
    val nonSplitRegex = ".*[/]{2,}.*".toRegex()

    if (nonMultiplicationRegex.matches(expression) || nonSplitRegex.matches(expression)) {
        println("Invalid expression")
        return false
    }
    return expressionVariablesRegex.matches(expression)
}

fun validateParenthesesFormat(rowInput: String): Boolean {
    var openParentheses = 0
    var closedParentheses = 0
    rowInput.forEach {
        if (it == '(') openParentheses++
        if (it == ')') closedParentheses++
        if (closedParentheses > openParentheses) {
            println("Invalid expression")
            return false
        }
    }
    return (openParentheses == closedParentheses) && (openParentheses > 0)
}


fun executeChainedOperationWithParentheses(rowInput: String) {
    var resultOperation = rowInput
    while (resultOperation.contains('(')) {
        resultOperation = solveInsideParenthesesOperation(resultOperation)
    }
    println(executeChainedOperation(splitOperation(resultOperation)))
}

fun solveInsideParenthesesOperation(operation: String): String {

    if (!operation.contains("(")) return operation
    if (!operation.contains(")")) return operation

    val expressionAfterFindVariables = replaceVariablesInExpression(operation)

    var startIndex = 0
    var endIndex = 0
    var insideOperation = ""
    findParentheses@ for (index in expressionAfterFindVariables.indices) {
        if (expressionAfterFindVariables[index] == '(') startIndex = index
        if (expressionAfterFindVariables[index] == ')') {
            endIndex = index
            insideOperation = expressionAfterFindVariables.substring(startIndex + 1, endIndex)
            break@findParentheses
        }
    }

    splitOperation(insideOperation)

    val resultFromInsideOperation =
        executeChainedOperation(splitOperation(insideOperation))
    return expressionAfterFindVariables.replace("(${insideOperation})", resultFromInsideOperation.toString())
}

fun splitOperation(rowOperation: String): Array<String> {
    var spacedOperation = rowOperation.replace("+", " + ")
    spacedOperation = spacedOperation.replace("-", " - ")
    spacedOperation = spacedOperation.replace("*", " * ")
    spacedOperation = spacedOperation.replace("/", " / ")
    val result = spacedOperation.trim().replace("\\s+".toRegex(), " ").split(" ").toTypedArray()
    return result
}

fun replaceVariablesInExpression(operation: String): String {
    var resultOperation = operation
    localMemory.forEach {
        if (operation.contains(it.key)) resultOperation = resultOperation.replace(it.key, it.value.toString())
    }
    return resultOperation
}

fun executeChainedOperationWithVariables(args: Array<String>): BigInteger? {

    val finalArgs = mutableListOf<String>()
    for (argument in args) {
        if (argument.first().isLetter()) {
            val foundElement = localMemory[argument]
            if (foundElement == null) {
                println("Unknown variable")
                return null
            } else {
                finalArgs.add(foundElement.toString())
            }
        } else {
            finalArgs.add(argument)
        }
    }
    return executeChainedOperation(finalArgs.toTypedArray())
}

fun executeSaveInLocalMemoryWithVariable(rowInput: String) {
    try {
        val operation = rowInput.replace("\\s+".toRegex(), "").split("=")
        if (operation.size > 2) {
            println("Invalid assignment")
            return
        }
        val foundValue = localMemory[operation[1]]
        if (foundValue != null) {
            localMemory[operation.first()] = foundValue
        } else {
            println("Unknown variable")
        }
    } catch (_: Exception) {
        println("Unknown variable")
    }
}

fun executeSaveInLocalMemory(rowInput: String) {
    try {
        val operation = rowInput.replace("\\s+".toRegex(), "").split("=")
        if (operation.size > 2) {
            println("Invalid assignment")
            return
        }
        localMemory[operation.first()] = BigInteger(operation[1])
    } catch (_: Exception) {
        println("Invalid assignment")
    }

}

fun findElementInLocalMemory(elementKey: String) {
    val foundElement = localMemory[elementKey]
    if (foundElement != null) {
        println(foundElement)
    } else {
        println("Unknown variable")
    }
}

fun executeChainedOperation(args: Array<String>): BigInteger {

    val expressionAfterHigherOperations = calculateHigherOperations(args)

    var finalResult = BigInteger.ZERO
    var nextOperation: Operation = Operation.SUM
    for (index in expressionAfterHigherOperations.indices) {
        if (index % 2 == 0) {
            //Is Number
            when (nextOperation) {
                Operation.SUM -> finalResult += BigInteger(expressionAfterHigherOperations[index])
                Operation.REST -> finalResult -= BigInteger(expressionAfterHigherOperations[index])
                else -> finalResult += BigInteger(expressionAfterHigherOperations[index])
            }
        } else {
            //Is Operation
            val currentArgument = expressionAfterHigherOperations[index]
            nextOperation = when {
                currentArgument.last() == '+' -> Operation.SUM
                currentArgument.last() == '-' && currentArgument.length % 2 != 0 -> Operation.REST
                else -> Operation.SUM
            }
        }
    }
    return finalResult
}

fun calculateHigherOperations(args: Array<String>): Array<String> {
    val result = mutableListOf<String>()
    var jumpNext = false
    for (index in args.indices) {
        if (jumpNext) {
            jumpNext = false
            continue
        }
        when (args[index]) {
            "*" -> {
                val currentResult = BigInteger(result[result.size - 1]) * BigInteger(args[index + 1])
                result.removeAt(result.size - 1)
                result.add(currentResult.toString())
                jumpNext = true
            }

            "/" -> {
                val currentResult = BigInteger(result[result.size - 1]) / BigInteger(args[index + 1])
                result.removeAt(result.size - 1)
                result.add(currentResult.toString())
                jumpNext = true
            }

            else -> {
                result.add(args[index])
            }
        }

    }

    return result.toTypedArray()
}

enum class Operation {
    SUM, REST
}

