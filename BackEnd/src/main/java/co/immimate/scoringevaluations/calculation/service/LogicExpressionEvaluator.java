package co.immimate.scoringevaluations.calculation.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for evaluating logic expressions in grid fields.
 * Supports complex expressions with AND, OR, comparison operators, and IN/NOT IN clauses.
 */
@Service
public class LogicExpressionEvaluator {
    
    private static final Logger logger = LoggerFactory.getLogger(LogicExpressionEvaluator.class);

    // Threshold for double comparison to account for floating point precision
    private static final double MATH_AB_NUMBER = 0.000001;
    
    // Logical operators
    private static final String OPERATOR_AND = "AND";
    private static final String OPERATOR_OR = "OR";
    
    // Comparison operators
    private static final String COMPARISON_EQUALS = "==";
    private static final String COMPARISON_NOT_EQUALS = "!=";
    private static final String COMPARISON_GREATER_THAN = ">";
    private static final String COMPARISON_LESS_THAN = "<";
    private static final String COMPARISON_GREATER_EQUALS = ">=";
    private static final String COMPARISON_LESS_EQUALS = "<=";
    private static final String COMPARISON_IN = "IN";
    private static final String COMPARISON_NOT_IN = "NOT IN";
    
    // Delimiters and separators
    private static final String EXPRESSION_SEPARATOR = ";";
    private static final String OR_SEPARATOR = " OR ";
    private static final String LIST_SEPARATOR = ",";
    private static final String LIST_START = "(";
    private static final String LIST_END = ")";
    
    // Log messages
    private static final String LOG_EXPRESSION_EVALUATION = "Expression evaluation: '{}' => {} because {}";
    
    /**
     * Class to track details about the evaluation of a logic expression.
     */
    public static class ExpressionEvaluation {
        private final StringBuilder explanation = new StringBuilder();
        private boolean result = false;
        
        public boolean getResult() {
            return result;
        }
        
        public void setResult(boolean result) {
            this.result = result;
        }
        
        public void addExplanation(String detail) {
            if (explanation.length() > 0) {
                explanation.append("; ");
            }
            explanation.append(detail);
        }
        
        public String getExplanation() {
            return explanation.toString();
        }
    }
    
    /**
     * Evaluates a logic expression with the given variable values.
     * 
     * @param expression The logic expression to evaluate
     * @param variables Map of variable names to their values
     * @return True if the expression evaluates to true, false otherwise
     */
    public boolean evaluateLogicExpression(String expression, Map<String, Object> variables) {
        return evaluateLogicExpression(expression, variables, null);
    }
    
    /**
     * Evaluates a logic expression with the given variable values and specified operator.
     * 
     * @param expression The logic expression to evaluate
     * @param variables Map of variable names to their values
     * @param operator The logical operator to use for semicolon-separated conditions ("AND" or "OR")
     * @return True if the expression evaluates to true, false otherwise
     */
    public boolean evaluateLogicExpression(String expression, Map<String, Object> variables, String operator) {
        ExpressionEvaluation evaluation = new ExpressionEvaluation();
        boolean result = evaluateLogicExpressionWithExplanation(expression, variables, operator, evaluation);
        
        // Log the detailed explanation for debugging
        logger.debug(LOG_EXPRESSION_EVALUATION, 
                    expression, result, evaluation.getExplanation());
                    
        return result;
    }
    
    /**
     * Evaluates a logic expression and provides a detailed explanation.
     * 
     * @param expression The logic expression to evaluate
     * @param variables Map of variable names to their values
     * @param operator The logical operator to use for semicolon-separated conditions
     * @param evaluation Object to collect evaluation details and explanations
     * @return True if the expression evaluates to true, false otherwise
     */
    public boolean evaluateLogicExpressionWithExplanation(String expression, Map<String, Object> variables, 
                                                         String operator, ExpressionEvaluation evaluation) {
        if (expression == null || expression.trim().isEmpty()) {
            evaluation.addExplanation("expression is empty");
            evaluation.setResult(false);
            return false;
        }
        
        // Check if the expression has embedded OR operators (explicit in the expression)
        if (expression.contains(OR_SEPARATOR)) {
            String[] orParts = expression.split(OR_SEPARATOR);
            boolean orResult = false;
            
            StringBuilder orExplanation = new StringBuilder("OR condition: ");
            for (int i = 0; i < orParts.length; i++) {
                ExpressionEvaluation partEval = new ExpressionEvaluation();
                boolean partResult = evaluateConditionWithExplanation(orParts[i].trim(), variables, partEval);
                
                if (i > 0) {
                    orExplanation.append(" OR ");
                }
                orExplanation.append("(").append(partEval.getExplanation()).append(")");
                
                if (partResult) {
                    orResult = true;
                }
            }
            
            evaluation.addExplanation(orExplanation + " => " + orResult);
            evaluation.setResult(orResult);
            return orResult;
        }
        
        // Split expressions by semicolon
        String[] conditions = expression.split(EXPRESSION_SEPARATOR);
        
        // If only one condition or no semicolons, just evaluate it
        if (conditions.length == 1) {
            return evaluateConditionWithExplanation(conditions[0].trim(), variables, evaluation);
        }
        
        // At this point, we have multiple conditions separated by semicolons
        // The operator parameter determines how to combine them
        
        // Default to AND if no operator is specified
        String defaultOperator = OPERATOR_AND;
        if (operator != null && !operator.trim().isEmpty()) {
            defaultOperator = operator.trim().toUpperCase();
        }
        
        // If operator contains semicolons, it means different operations for different parts
        String[] operators = defaultOperator.split(EXPRESSION_SEPARATOR);
        
        // Ensure we have enough operators (use the last one for any extras)
        String[] effectiveOperators = new String[conditions.length - 1];
        for (int i = 0; i < effectiveOperators.length; i++) {
            if (i < operators.length) {
                effectiveOperators[i] = operators[i].trim().toUpperCase();
            } else {
                // If fewer operators than needed, use the last one for remaining conditions
                effectiveOperators[i] = operators[operators.length - 1].trim().toUpperCase();
            }
        }
        
        // Evaluate all conditions and combine them according to the operators
        boolean result = evaluateConditionWithExplanation(conditions[0].trim(), variables, evaluation);
        
        StringBuilder compoundExplanation = new StringBuilder();
        compoundExplanation.append("(").append(evaluation.getExplanation()).append(")");
        
        for (int i = 1; i < conditions.length; i++) {
            String currentCondition = conditions[i].trim();
            String currentOperator = effectiveOperators[i - 1];
            
            ExpressionEvaluation currentEval = new ExpressionEvaluation();
            boolean currentResult = evaluateConditionWithExplanation(currentCondition, variables, currentEval);
            
            compoundExplanation.append(" ").append(currentOperator).append(" ");
            compoundExplanation.append("(").append(currentEval.getExplanation()).append(")");
            
            if (currentOperator.equals(OPERATOR_OR)) {
                result = result || currentResult;
            } else {
                // Default to AND for any unrecognized operator
                result = result && currentResult;
            }
        }
        
        evaluation.addExplanation(compoundExplanation + " => " + result);
        evaluation.setResult(result);
        return result;
    }
    
    /**
     * Evaluates a single condition with variables and provides explanation.
     * 
     * @param condition The condition to evaluate
     * @param variables Map of variable names to their values
     * @param evaluation Object to collect evaluation details
     * @return True if the condition evaluates to true, false otherwise
     */
    private boolean evaluateConditionWithExplanation(String condition, Map<String, Object> variables, ExpressionEvaluation evaluation) {
        // Clean up the condition
        condition = condition.trim();
        if (condition.isEmpty()) {
            evaluation.addExplanation("empty condition");
            evaluation.setResult(false);
            return false;
        }
        
        // Check for IN and NOT IN operations
        if (condition.contains(COMPARISON_IN) && !condition.contains(COMPARISON_NOT_IN)) {
            return evaluateInOperation(condition, variables, evaluation);
        }
        
        if (condition.contains(COMPARISON_NOT_IN)) {
            return evaluateNotInOperation(condition, variables, evaluation);
        }
        
        // Look for comparison operators (==, !=, >, <, >=, <=)
        if (condition.contains(COMPARISON_EQUALS)) {
            String[] parts = condition.split(COMPARISON_EQUALS, 2);
            return compareValues(parts[0].trim(), parts[1].trim(), COMPARISON_EQUALS, variables, evaluation);
        } else if (condition.contains(COMPARISON_NOT_EQUALS)) {
            String[] parts = condition.split(COMPARISON_NOT_EQUALS, 2);
            return compareValues(parts[0].trim(), parts[1].trim(), COMPARISON_NOT_EQUALS, variables, evaluation);
        } else if (condition.contains(COMPARISON_GREATER_EQUALS)) {
            String[] parts = condition.split(COMPARISON_GREATER_EQUALS, 2);
            return compareValues(parts[0].trim(), parts[1].trim(), COMPARISON_GREATER_EQUALS, variables, evaluation);
        } else if (condition.contains(COMPARISON_LESS_EQUALS)) {
            String[] parts = condition.split(COMPARISON_LESS_EQUALS, 2);
            return compareValues(parts[0].trim(), parts[1].trim(), COMPARISON_LESS_EQUALS, variables, evaluation);
        } else if (condition.contains(COMPARISON_GREATER_THAN)) {
            String[] parts = condition.split(COMPARISON_GREATER_THAN, 2);
            return compareValues(parts[0].trim(), parts[1].trim(), COMPARISON_GREATER_THAN, variables, evaluation);
        } else if (condition.contains(COMPARISON_LESS_THAN)) {
            String[] parts = condition.split(COMPARISON_LESS_THAN, 2);
            return compareValues(parts[0].trim(), parts[1].trim(), COMPARISON_LESS_THAN, variables, evaluation);
        }
        
        // If no operator is found, just check if the variable exists and is truthy
        Object value = resolveValue(condition, variables);
        boolean result = isTruthyValue(value);
        
        if (value == null) {
            evaluation.addExplanation("variable '" + condition + "' was not found");
        } else {
            evaluation.addExplanation("variable '" + condition + "' is " + value + " (truthy: " + result + ")");
        }
        
        evaluation.setResult(result);
        return result;
    }
    
    /**
     * Evaluates an "IN" operation, checking if a value is in a list.
     * 
     * @param condition The IN condition to evaluate
     * @param variables Map of variable names to their values
     * @param evaluation Object to collect evaluation details
     * @return True if the value is in the list, false otherwise
     */
    private boolean evaluateInOperation(String condition, Map<String, Object> variables, ExpressionEvaluation evaluation) {
        // Format: variable IN (value1, value2, ...)
        String[] parts = condition.split(COMPARISON_IN, 2);
        String variable = parts[0].trim();
        String listPart = parts[1].trim();
        
        // Extract the list part (remove parentheses)
        if (listPart.startsWith(LIST_START) && listPart.endsWith(LIST_END)) {
            listPart = listPart.substring(1, listPart.length() - 1);
        }
        
        // Split the list by commas
        String[] listItems = listPart.split(LIST_SEPARATOR);
        Object variableValue = resolveValue(variable, variables);
        
        boolean result = false;
        StringBuilder explanation = new StringBuilder("'" + variable + "' (");
        explanation.append(variableValue != null ? variableValue : "null");
        explanation.append(") IN (");
        
        for (int i = 0; i < listItems.length; i++) {
            String item = listItems[i].trim();
            Object itemValue = resolveValue(item, variables);
            
            if (i > 0) {
                explanation.append(", ");
            }
            explanation.append(itemValue != null ? itemValue : "null");
            
            if (areValuesEqual(variableValue, itemValue)) {
                result = true;
            }
        }
        
        explanation.append(") => ").append(result);
        evaluation.addExplanation(explanation.toString());
        evaluation.setResult(result);
        return result;
    }
    
    /**
     * Evaluates a "NOT IN" operation, checking if a value is not in a list.
     * 
     * @param condition The NOT IN condition to evaluate
     * @param variables Map of variable names to their values
     * @param evaluation Object to collect evaluation details
     * @return True if the value is not in the list, false otherwise
     */
    private boolean evaluateNotInOperation(String condition, Map<String, Object> variables, ExpressionEvaluation evaluation) {
        // Format: variable NOT IN (value1, value2, ...)
        String[] parts = condition.split(COMPARISON_NOT_IN, 2);
        String variable = parts[0].trim();
        String listPart = parts[1].trim();
        
        // Extract the list part (remove parentheses)
        if (listPart.startsWith(LIST_START) && listPart.endsWith(LIST_END)) {
            listPart = listPart.substring(1, listPart.length() - 1);
        }
        
        // Split the list by commas
        String[] listItems = listPart.split(LIST_SEPARATOR);
        Object variableValue = resolveValue(variable, variables);
        
        boolean inList = false;
        StringBuilder explanation = new StringBuilder("'" + variable + "' (");
        explanation.append(variableValue != null ? variableValue : "null");
        explanation.append(") NOT IN (");
        
        for (int i = 0; i < listItems.length; i++) {
            String item = listItems[i].trim();
            Object itemValue = resolveValue(item, variables);
            
            if (i > 0) {
                explanation.append(", ");
            }
            explanation.append(itemValue != null ? itemValue : "null");
            
            if (areValuesEqual(variableValue, itemValue)) {
                inList = true;
            }
        }
        
        boolean result = !inList;
        explanation.append(") => ").append(result);
        evaluation.addExplanation(explanation.toString());
        evaluation.setResult(result);
        return result;
    }
    
    /**
     * Compares two values based on the specified operator.
     * 
     * @param leftSide The left side of the comparison (typically variable name)
     * @param rightSide The right side of the comparison (typically a value)
     * @param operator The comparison operator (==, !=, >, <, >=, <=)
     * @param variables Map of variable names to their values
     * @param evaluation Object to collect evaluation details
     * @return The result of the comparison
     */
    private boolean compareValues(String leftSide, String rightSide, String operator, 
                                 Map<String, Object> variables, ExpressionEvaluation evaluation) {
        Object leftValue = resolveValue(leftSide, variables);
        Object rightValue = resolveValue(rightSide, variables);
        
        // If rightValue is null, try to interpret rightSide as a literal
        if (rightValue == null) {
            // Remove quotes if present
            rightValue = rightSide.replaceAll("^['\"]|['\"]$", "");
        }
        
        StringBuilder explanation = new StringBuilder("'" + leftSide + "' (");
        explanation.append(leftValue != null ? leftValue : "null");
        explanation.append(") ").append(operator).append(" '").append(rightValue).append("'");
        
        // Handle null cases
        if (leftValue == null) {
            boolean result = false;
            if (operator.equals(COMPARISON_EQUALS)) {
                result = rightValue == null;
            } else if (operator.equals(COMPARISON_NOT_EQUALS)) {
                result = rightValue != null;
            }
            
            explanation.append(" => ").append(result);
            evaluation.addExplanation(explanation.toString());
            evaluation.setResult(result);
            return result;
        }
        
        // Handle numeric comparison
        if (isNumeric(leftValue) && isNumeric(rightValue)) {
            double leftNum = convertToDouble(leftValue);
            double rightNum = convertToDouble(rightValue);
            
            boolean result;
            result = switch (operator) {
                case COMPARISON_EQUALS -> Math.abs(leftNum - rightNum) < MATH_AB_NUMBER;
                case COMPARISON_NOT_EQUALS -> Math.abs(leftNum - rightNum) >= MATH_AB_NUMBER;
                case COMPARISON_GREATER_THAN -> leftNum > rightNum;
                case COMPARISON_LESS_THAN -> leftNum < rightNum;
                case COMPARISON_GREATER_EQUALS -> leftNum >= rightNum;
                case COMPARISON_LESS_EQUALS -> leftNum <= rightNum;
                default -> false;
            };
            
            explanation.append(" => ").append(result);
            evaluation.addExplanation(explanation.toString());
            evaluation.setResult(result);
            return result;
        }
        
        // Handle string comparison
        String leftStr = leftValue.toString();
        String rightStr = rightValue.toString();
        
        boolean result;
        switch (operator) {
            case COMPARISON_EQUALS -> result = leftStr.equalsIgnoreCase(rightStr);
            case COMPARISON_NOT_EQUALS -> result = !leftStr.equalsIgnoreCase(rightStr);
            default -> {
                result = false;
                explanation.append(" (unsupported operator for string comparison)");
            }
        }
        
        explanation.append(" => ").append(result);
        evaluation.addExplanation(explanation.toString());
        evaluation.setResult(result);
        return result;
    }
    
    /**
     * Checks if a value is numeric.
     * 
     * @param value The value to check
     * @return True if the value is numeric, false otherwise
     */
    private boolean isNumeric(Object value) {
        if (value instanceof Number) {
            return true;
        } else if (value instanceof String string) 
        {
            try 
            {
                Double.valueOf(string);
                return true;
            } 
            catch (NumberFormatException e) 
            {
                return false;
            }
        }
        return false;
    }
    
    /**
     * Converts an object to a double value.
     * 
     * @param value The value to convert
     * @return The double value
     */
    private double convertToDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String string) {
            return Double.parseDouble(string);
        }
        return 0.0;
    }
    
    /**
     * Checks if two values are equal.
     * 
     * @param value1 The first value
     * @param value2 The second value
     * @return True if the values are equal, false otherwise
     */
    private boolean areValuesEqual(Object value1, Object value2) {
        if (value1 == null && value2 == null) {
            return true;
        }
        if (value1 == null || value2 == null) {
            return false;
        }
        if (value1 instanceof Number && value2 instanceof Number) {
            double num1 = ((Number) value1).doubleValue();
            double num2 = ((Number) value2).doubleValue();
            return Math.abs(num1 - num2) < MATH_AB_NUMBER;
        } else if (value1 instanceof String && value2 instanceof String) {
            String str1 = (String) value1;
            String str2 = (String) value2;
            return str1.equalsIgnoreCase(str2);
        } else {
            return value1.equals(value2);
        }
    }
    
    /**
     * Checks if a value is truthy.
     * 
     * @param value The value to check
     * @return True if the value is truthy, false otherwise
     */
    private boolean isTruthyValue(Object value) {
        if (value == null) {
            return false;
        }
        
        // Using enhanced instanceof pattern matching
        if (value instanceof Boolean aBoolean) {
            return aBoolean;
        }
        if (value instanceof Number number) {
            return number.doubleValue() != 0;
        }
        if (value instanceof String str) {
            return !str.isEmpty();
        }
        return true;
    }
    
    /**
     * Resolves a value from the variables map.
     * 
     * @param key The key to resolve
     * @param variables Map of variable names to their values
     * @return The resolved value
     */
    private Object resolveValue(String key, Map<String, Object> variables) {
        return variables.get(key);
    }
}