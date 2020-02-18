import java.util.Date;

class BuildIndexDocument1000Genomes {
    public static void main(String... args) {
        if (args.length != 2) {
            System.out.println("Bad number of arguments (" + args.length + "), expected 2 (snapshot_id, root_row_id).");
            System.exit(1); // error
        }

        String jsonStr = "{ "
                + "\"date_created\":\"" + (new Date()) + "\", "
                + "\"snapshot_id\":\"" + args[0] + "\", "
                + "\"root_row_id\":\"" + args[1] + "\""
                + "}";

        System.out.println(jsonStr);
        System.exit(0); // success
    }
}