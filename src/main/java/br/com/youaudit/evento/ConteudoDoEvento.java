package br.com.youaudit.evento;

import jakarta.validation.Valid;

import java.util.List;

/** A entrega do produto, como chega no evento. */
public record ConteudoDoEvento(@Valid List<ItemDoEvento> itens) {
}
