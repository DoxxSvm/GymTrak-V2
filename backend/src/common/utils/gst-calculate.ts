type GSTResult = {
    baseAmount: number;
    gstAmount: number;
    totalAmount: number;
  };
  
  export function GSTCalculate(
    amount: number,
    gstRate: number,
    isInclusive: boolean = false
  ): GSTResult {
    let gstAmount: number;
    let baseAmount: number;
    let totalAmount: number;
  
    if (isInclusive) {
      // GST already included
      baseAmount = amount / (1 + gstRate / 100);
      gstAmount = amount - baseAmount;
      totalAmount = amount;
    } else {
      // GST added on top
      gstAmount = (amount * gstRate) / 100;
      baseAmount = amount;
      totalAmount = amount + gstAmount;
    }
  
    return {
      baseAmount: Number(baseAmount.toFixed(2)),
      gstAmount: Number(gstAmount.toFixed(2)),
      totalAmount: Number(totalAmount.toFixed(2)),
    };
  }
  
  // Usage Examples:
  
  // Exclusive GST (₹1000 + 18%)
  const result1 = GSTCalculate(1000, 18);
  
  // Inclusive GST (₹1180 includes 18%)
  const result2 = GSTCalculate(1180, 18, true);
  
  console.log(result1, result2);