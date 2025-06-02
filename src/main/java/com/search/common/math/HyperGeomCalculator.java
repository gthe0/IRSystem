package com.search.common.math;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class HyperGeomCalculator {

    private static List<BigInteger> factorialCache = new ArrayList<>();

    public static double calculateHyperGeomProb(int n, int x, int r, int z)
    {
        // Invalid arguments, won't throw exception
        if (z > x || r > n || z > r) {
            return 0.0; 
        }

        BigInteger numerator   = binomialCoef(x, z).multiply(binomialCoef(n - x, r - z));
        BigInteger denominator = binomialCoef(n, r);

        return (numerator.doubleValue()/denominator.doubleValue());
    }

    private static BigInteger binomialCoef(int n, int k)
    {   
        if (k > n || k < 0) {
            return BigInteger.ZERO;
        }

        BigInteger numerator    = factorial(n);
        BigInteger denominator  = factorial(n-k).multiply(factorial(k));

        return numerator.divide(denominator);
    }

    private static void incFactorialCache(int max) {
        
        if(factorialCache.isEmpty()) factorialCache.add(BigInteger.ONE);
        if(max < factorialCache.size()) return;

        for (int i = factorialCache.size(); i <= max; i++) {
            factorialCache.add(factorialCache.get(i - 1).multiply(BigInteger.valueOf(i)));
        }
    }
    
    private static BigInteger factorial(int i) {
        incFactorialCache(i);
        return factorialCache.get(i);
    }
}
