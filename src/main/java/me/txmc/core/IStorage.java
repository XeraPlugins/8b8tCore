package me.txmc.core;

/**
 * @author 254n_m
 * @since 2023/03/04 12:04 AM
 * This file was created as a part of L2X9RebootCore
 */
public interface IStorage<T, F> {
    void save(T t, F f);

    T load(F f);

    void delete(F f);

}