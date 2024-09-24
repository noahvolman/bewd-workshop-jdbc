package com.example.demo;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SnackResultSetExtractor implements ResultSetExtractor<List<Snack>> {
    @Override
    public List<Snack> extractData(ResultSet rs) throws SQLException, DataAccessException {
        List<Snack> snacks = new ArrayList<Snack>();
        while (rs.next()) {
            Snack s = new Snack();
            s.snacknr = rs.getInt("snacknr");
            s.snacknaam = rs.getString("snacknaam");
            s.calorieen= rs.getInt("calorieen");
            snacks.add(s);
        }
        return snacks;

    }
}