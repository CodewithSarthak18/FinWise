package com.finwise.service;

import com.finwise.dto.whatif.CategorySpendingOption;
import com.finwise.dto.whatif.WhatIfPresetScenario;
import com.finwise.dto.whatif.WhatIfSimulationRequest;
import com.finwise.dto.whatif.WhatIfSimulationResponse;

import java.time.YearMonth;
import java.util.List;

public interface WhatIfCalculatorService {

    WhatIfSimulationResponse simulateSavings(Long userId, WhatIfSimulationRequest request);

    List<CategorySpendingOption> getSpendingCategories(Long userId, YearMonth month);

    List<WhatIfPresetScenario> getPresetScenarios(Long userId, Long categoryId, YearMonth month);
}
