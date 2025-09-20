public class FactorialCalculator {

    /**
     * Calculates the factorial of a non-negative integer recursively.
     *
     * @param n The non-negative integer for which to calculate the factorial.
     * @return The factorial of the given number.
     */
    public static long calculateFactorial(int n) {
        // Base case: Factorial of 0 or 1 is 1
        if (n == 0 || n == 1) {
            return 1;
        }
        // Recursive case: n * factorial(n-1)
        else {
            return n * calculateFactorial(n - 1);
        }
    }
    
    
    /**
     * Calculates the factorial of a non-negative integer through a for-loop.
     *
     * @param n The non-negative integer for which to calculate the factorial.
     * @return The factorial of the given number.
     */
    public static long calculateFactorial_loop(int n) {
        long ans=1;
        for(int i=1; i<=n; i++)
            ans *=i;
       
        return ans;
    }
    

    public static void main(String[] args) {
        int number = 5;
        long factorialResult = calculateFactorial(number);
        System.out.println("The factorial of " + number + " is: " + factorialResult); // Output: The factorial of 5 is: 120

        number = 5;
        factorialResult = calculateFactorial_loop(number);
        System.out.println("The factorial of " + number + " is: " + factorialResult); // Output: The factorial of 0 is: 1
    }
}