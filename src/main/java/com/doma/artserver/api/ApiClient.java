package com.doma.artserver.api;

import java.net.URI;
import java.util.List;

public interface ApiClient<T> {
    List<T> fetchItems(int page);
    URI generateUrl(int page);
}
