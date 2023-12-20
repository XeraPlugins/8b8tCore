package me.txmc.core.home;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author 254n_m
 * @since 2023/12/19 8:28 PM
 * This file was created as a part of 8b8tCore
 */
@Getter
@RequiredArgsConstructor
public class HomeData {
    private final List<Home> homes;
    public void addHome(Home home) {
        homes.add(home);
    }

    public void deleteHome(Home home) {
        homes.remove(home);
    }
    public Stream<Home> stream() {
        return homes.stream();
    }

    /**
     * Check if there are saved homes
     * @return true if there are homes false otherwise
     */
   public boolean hasHomes() {
        return homes.isEmpty();
   }
}
