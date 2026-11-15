package com.example.footprint.data.model;

import java.util.ArrayList;
import java.util.List;

public final class UserPage {
    public List<UserSummary> content = new ArrayList<>();
    public int page;
    public int size;
    public boolean hasNext;
}
