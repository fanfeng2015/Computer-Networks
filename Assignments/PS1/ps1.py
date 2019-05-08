import math

def compute(k):
   lmbd = 16722.0 / 24 / 60 / 60 # per sec
   mu = 1.0 / 3 / 60             # per sec
   denominator = 0.0
   for i in range(k):
       denominator += ((lmbd / mu) ** i) / math.factorial(i)
   result = ((lmbd / mu) ** k) / math.factorial(k) / denominator
   print("k = ", k, ", result = ", result, sep="")

def choose(n, k):
    k = min(n - k, k)
    if (k == 0):
        return 1
    return math.factorial(n) / math.factorial(k) / math.factorial(n - k)

if __name__ == '__main__':
##    for i in range(1, 50):
##        compute(i)
    sum = 0.0
    for i in range(21):
        sum += choose(120, i) * (0.1 ** i) * (0.9 ** (120 - i))
    print(1.0 - sum)
