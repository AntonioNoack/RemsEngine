package opencv;

public class Inverse {

    public static double[] invert(double[] a, int n) {

        double[] x = new double[n * n];
        double[] b = new double[n * n];

        int[] index = new int[n];
        for (int i = 0; i < n; ++i)
            b[i * n + i] = 1;

        // Transform the matrix into an upper triangle
        gaussian(a, index);

        // Update the matrix b[i][j] with the ratios stored
        for (int i = 0; i < n - 1; ++i)
            for (int j = i + 1; j < n; ++j)
                for (int k = 0; k < n; ++k)
                    b[index[j] * n + k] -= a[index[j] * n + i] * b[index[i] * n + k];

        // Perform backward substitutions
        for (int i = 0; i < n; ++i) {
            x[(n - 1) * n + i] = b[index[n - 1] * n + i] / a[index[n - 1] * n + n - 1];
            for (int j = n - 2; j >= 0; --j) {
                x[j * n + i] = b[index[j] * n + i];
                for (int k = j + 1; k < n; ++k) {
                    x[j * n + i] -= a[index[j] * n + k] * x[k * n + i];
                }
                x[j * n + i] /= a[index[j] * n + j];
            }
        }

        return x;

    }

    // Method to carry out the partial-pivoting Gaussian
    // elimination. Here index[] stores pivoting order.
    public static void gaussian(double[] a, int[] index) {

        int n = index.length;

        double[] c = new double[n];

        // Initialize the index
        for (int i = 0; i < n; ++i) {
            index[i] = i;
        }

        // Find the rescaling factors, one from each row
        for (int i = 0; i < n; ++i) {
            double c1 = 0;
            for (int j = 0; j < n; ++j) {
                double c0 = Math.abs(a[i * n + j]);
                if (c0 > c1) c1 = c0;
            }
            c[i] = c1;
        }

        // Search the pivoting element from each column
        int k = 0;
        for (int j = 0; j < n - 1; ++j) {

            double pi1 = 0;
            for (int i = j; i < n; ++i) {
                double pi0 = Math.abs(a[index[i] * n + j]);
                pi0 /= c[index[i]];
                if (pi0 > pi1) {
                    pi1 = pi0;
                    k = i;
                }
            }

            // Interchange rows according to the pivoting order
            int iTmp = index[j];
            index[j] = index[k];
            index[k] = iTmp;

            for (int i = j + 1; i < n; ++i) {

                double pj = a[index[i] * n + j] / a[index[j] * n + j];

                // Record pivoting ratios below the diagonal
                a[index[i] * n + j] = pj;

                // Modify other elements accordingly
                for (int l = j + 1; l < n; ++l) {
                    a[index[i] * n + l] -= pj * a[index[j] * n + l];
                }

            }
        }
    }

    public static void invert(float[] a, float[] x, int n) {

        float[] b = new float[n * n];

        for (int i = 0; i < n * n; i++) x[i] = 0f;

        int[] index = new int[n];
        for (int i = 0; i < n; ++i)
            b[i * n + i] = 1;

        // Transform the matrix into an upper triangle
        gaussian(a, index);

        // Update the matrix b[i][j] with the ratios stored
        for (int i = 0; i < n - 1; ++i)
            for (int j = i + 1; j < n; ++j)
                for (int k = 0; k < n; ++k)
                    b[index[j] * n + k] -= a[index[j] * n + i] * b[index[i] * n + k];

        // Perform backward substitutions
        for (int i = 0; i < n; ++i) {
            x[(n - 1) * n + i] = b[index[n - 1] * n + i] / a[index[n - 1] * n + n - 1];
            for (int j = n - 2; j >= 0; --j) {
                x[j * n + i] = b[index[j] * n + i];
                for (int k = j + 1; k < n; ++k) {
                    x[j * n + i] -= a[index[j] * n + k] * x[k * n + i];
                }
                x[j * n + i] /= a[index[j] * n + j];
            }
        }

    }

    // Method to carry out the partial-pivoting Gaussian
    // elimination. Here index[] stores pivoting order.
    public static void gaussian(float[] a, int[] index) {

        int n = index.length;

        float[] c = new float[n];

        // Initialize the index
        for (int i = 0; i < n; ++i) {
            index[i] = i;
        }

        // Find the rescaling factors, one from each row
        for (int i = 0; i < n; ++i) {
            float c1 = 0;
            for (int j = 0; j < n; ++j) {
                float c0 = Math.abs(a[i * n + j]);
                if (c0 > c1) c1 = c0;
            }
            c[i] = c1;
        }

        // Search the pivoting element from each column
        int k = 0;
        for (int j = 0; j < n - 1; ++j) {

            float pi1 = 0;
            for (int i = j; i < n; ++i) {
                float pi0 = Math.abs(a[index[i] * n + j]);
                pi0 /= c[index[i]];
                if (pi0 > pi1) {
                    pi1 = pi0;
                    k = i;
                }
            }

            // Interchange rows according to the pivoting order
            int iTmp = index[j];
            index[j] = index[k];
            index[k] = iTmp;

            for (int i = j + 1; i < n; ++i) {

                float pj = a[index[i] * n + j] / a[index[j] * n + j];

                // Record pivoting ratios below the diagonal
                a[index[i] * n + j] = pj;

                // Modify other elements accordingly
                for (int l = j + 1; l < n; ++l) {
                    a[index[i] * n + l] -= pj * a[index[j] * n + l];
                }

            }
        }
    }
}