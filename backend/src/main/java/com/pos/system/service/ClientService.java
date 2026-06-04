package com.pos.system.service;

import com.pos.system.dto.request.ClientRequest;
import com.pos.system.dto.response.ClientResponse;

import java.util.List;

public interface ClientService {

    ClientResponse create(ClientRequest request);

    ClientResponse getById(Long id);

    ClientResponse update(Long id, ClientRequest request);

    List<ClientResponse> search(String query);

    void delete(Long id);
}
