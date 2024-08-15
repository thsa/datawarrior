package com.actelion.research.datawarrior.task.jep;

import com.actelion.research.util.DoubleFormat;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import java.util.Stack;

public class JEPNumFunction extends PostfixMathCommand {
    public JEPNumFunction() {
        numberOfParameters = 1;
    }

    public void run(Stack inStack) throws ParseException {
        checkStack(inStack);// check the stack
        Object param = inStack.pop();

        double value = Double.NaN;
        if (param instanceof String) {
            try {
                value = Double.parseDouble((String)param);
            } catch (NumberFormatException nfe) {}
        }
        else if (param instanceof Double) {
            value = (Double)param;
        }

        inStack.push(value);
        }
    }

