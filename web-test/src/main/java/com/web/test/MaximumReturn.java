package com.web.test;

public class MaximumReturn {

    public static void main(String[] args) {
        int[] data = {5,6,8,2,4,1};
        maxReturn(data);
    }

    public static void maxReturn(int[] data) {
        int length = data.length;
        int index = 0;
        int maxReturn = 0;
        int buyIndex = 0;
        for (int j = 1; j < length; j++) {
            if (maxReturn < data[j] - data[index]) {
                maxReturn = data[j] - data[index];
                buyIndex = index;
            }
            if (data[j] < data[buyIndex]) {
                index = j;
            }
        }
        System.out.print("买入下标:" + buyIndex + ".最大收益:" + maxReturn);
    }
}
