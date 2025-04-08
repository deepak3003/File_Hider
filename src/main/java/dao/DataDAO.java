package dao;

import db.MyConnection;
import model.Data;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DataDAO {

    public static List<Data> getAllFiles(String email) throws SQLException {
        Connection connection = MyConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement("SELECT * FROM data WHERE email = ?");
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();

        List<Data> files = new ArrayList<>();
        while (rs.next()) {
            int id = rs.getInt(1);
            String name = rs.getString(2);
            String path = rs.getString(3);
            files.add(new Data(id, name, path));
        }

        return files;
    }

    public static int hideFile(Data file) throws SQLException, IOException {
        File f = new File(file.getPath());

        // ✅ Step 1: Check if the file exists
        if (!f.exists()) {
            System.out.println("❌ Error: File not found at " + f.getAbsolutePath());
            return 0;
        }

        // ✅ Step 2: Check if the file is readable
        if (!f.canRead()) {
            System.out.println("❌ Error: File exists but cannot be read.");
            return 0;
        }

        // ✅ Step 3: Establish DB connection and insert file
        Connection connection = MyConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement("INSERT INTO data(name, path, email, bin_data) VALUES (?, ?, ?, ?)");

        ps.setString(1, file.getFileName());
        ps.setString(2, file.getPath());
        ps.setString(3, file.getEmail());

        FileReader fr = new FileReader(f);
        ps.setCharacterStream(4, fr, f.length());

        int ans = ps.executeUpdate();
        fr.close();

        // ✅ Step 4: Delete the file only if insert was successful
        if (ans > 0) {
            boolean deleted = f.delete();
            if (!deleted) {
                System.out.println("⚠️ Warning: File was saved but could not be deleted.");
            }
        }

        return ans;
    }

    public static void unhide(int id) throws SQLException, IOException {
        Connection connection = MyConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement("SELECT path, bin_data FROM data WHERE id = ?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            String path = rs.getString("path");
            Clob c = rs.getClob("bin_data");

            Reader r = c.getCharacterStream();
            FileWriter fw = new FileWriter(path);

            int i;
            while ((i = r.read()) != -1) {
                fw.write((char) i);
            }

            fw.close();

            ps = connection.prepareStatement("DELETE FROM data WHERE id = ?");
            ps.setInt(1, id);
            ps.executeUpdate();

            System.out.println("✅ Successfully Unhidden");
        } else {
            System.out.println("❌ Error: No record found with ID = " + id);
        }
    }
}
