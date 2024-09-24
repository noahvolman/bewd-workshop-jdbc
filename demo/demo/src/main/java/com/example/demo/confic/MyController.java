package com.example.demo.confic;

import com.example.demo.Snack;
import com.example.demo.SnackResultSetExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static jdk.internal.org.jline.reader.impl.LineReaderImpl.CompletionType.List;

@RestController
public class MyController {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/testdb")
    public String testDB() {
        jdbcTemplate.execute("CREATE TABLE box (id INT NOT NULL)");
        jdbcTemplate.execute("INSERT INTO box VALUES (1)");
        jdbcTemplate.execute("INSERT INTO box VALUES (2)");
        int value = jdbcTemplate.queryForObject("SELECT MIN(id) FROM box", Integer.class);
        return String.valueOf(value);
    }

    @GetMapping("/testdata")
    public String testData() {
        return jdbcTemplate.queryForObject("SELECT snacknaam FROM Snack WHERE snacknr = 1", String.class);
    }
}