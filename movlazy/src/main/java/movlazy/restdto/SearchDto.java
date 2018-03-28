package movlazy.restdto;

import movlazy.dto.SearchItemDto;

public class SearchDto {

    private final SearchItemDto[] results;

    public SearchDto(SearchItemDto[] results) {
        this.results = results;
    }

    public SearchItemDto[] getResults() {
        return results;
    }
}
