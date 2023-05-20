package cz.it4i.fiji.datastore.TestUI;
import java.util.Scanner;

public class TestUI {
    public static boolean checkTerminate(String input) {
        return input.equals("quit");
    }
    private static void performAction(Scanner scanner,String entity) {
        int editChoice = 0;
        while (editChoice != 4) {
            System.out.println("===== " + entity + " Menu =====");
            System.out.println("1. Create");
            System.out.println("2. Update");
            System.out.println("3. Delete");
            System.out.println("4. Back");
            System.out.println("5. Quit");
            System.out.println("========================");

            editChoice = scanner.nextInt();

            switch (editChoice) {
                case 1:
                    if(entity.equals("OAuthServer")) {
                        OAuthServerIF.createServerForm(scanner);
                    }
                    else if(entity.equals("OAuthUser"))
                    {
                        OAuthUserIF.createUser(scanner);
                    }
                    else if(entity.equals("OAuthGroup"))
                    {
                        OAuthGroupIF.createGroup(scanner);
                    }
                    break;
                case 2:
                    if(entity.equals("OAuthServer")) {
                        OAuthServerIF.updateForm(scanner);
                    }
                    else if(entity.equals("OAuthUser")) {
                        OAuthUserIF.updateForm(scanner);
                    }
                    else if(entity.equals("OAuthGroup"))
                    {
                        OAuthGroupIF.updateGroupForm(scanner);
                    }
                    break;
                case 3:
                    if(entity.equals("OAuthServer")) {
                        OAuthServerIF.deleteForm(scanner);
                    }
                    else if(entity.equals("OAuthUser")) {
                        OAuthUserIF.deleteUser(scanner);
                    }
                    else if(entity.equals("OAuthGroup"))
                    {
                        OAuthGroupIF.deleteGroup(scanner);
                    }
                    break;
                case 4:
                    break;
                case 5:
                    System.out.println("Quiting...");
                    System.exit(0);
                default:
                    System.out.println("Invalid choice. Try again.");
                    break;
            }

            System.out.println();
        }
    }
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        int choice = 0;
        while (choice != 4) {
            System.out.println("===== Menu =====");
            System.out.println("1. OAuthServer");
            System.out.println("2. OAuthUser");
            System.out.println("3. OAuthGroup");
            System.out.println("4. Quit");
            System.out.println("================");

            choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    performAction(scanner,"OAuthServer");
                    break;
                case 2:
                    performAction(scanner,"OAuthUser");
                    break;
                case 3:
                    performAction(scanner,"OAuthGroup");
                    break;
                case 4:
                    System.out.println("Exiting...");
                    break;
                default:
                    System.out.println("Invalid choice. Try again.");
                    break;
            }

            System.out.println();
        }

        scanner.close();
    }

}
