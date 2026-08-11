package com.libris.book;

/**
 * Availability of a copy in the catalogue.
 *
 * <p>RESERVADO is not "available to anyone": the book is being held for the reader that
 * was first in the waiting list, and only that reader may borrow it.
 */
public enum BookStatus {

    DISPONIBLE,
    PRESTADO,
    RESERVADO
}
