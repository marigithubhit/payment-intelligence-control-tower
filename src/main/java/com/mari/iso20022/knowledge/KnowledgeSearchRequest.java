package com.mari.iso20022.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KnowledgeSearchRequest(@NotBlank @Size(max = 200) String q) { }
