package com.example.vmsobd2;

public class ObdFormula {
    public String pid;
    public int hexCount;
    public String formula;

    public ObdFormula(String pid, int hexCount, String formula) {
        this.pid = pid;
        this.hexCount = hexCount;
        this.formula = formula;
    }
}

