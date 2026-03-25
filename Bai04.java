import java.util.Scanner;

public class Bai04{
    static void main(String[] args) {
        Scanner sc= new Scanner(System.in);
        System.out.println("Nhap n:");
        int n = sc.nextInt();
        int a=0,b=1;
        for (int i = 0 ; i<n ; i++){
            int temp = a+b;
            a=b;
            b=temp;
        }
        System.out.println("Số fibonacci thứ " + n + " là : " + a);
        sc.close();
    }
}
