export interface Pageable {
    offset: number;
    pageNumber: number;
    pageSize: number;
    paged: boolean;
    sort: Sort;
    unpaged: boolean;
    totalPages: number;
    sortField: SortField
}

export type Sort = {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
}

export type SortField = {
    field: string;
    order: string;
    enable: boolean;
}

export type Language = {
    lang: string,
    name: string
}

export interface Feed {
    id: string;
    title: string;
    xmlUrl: string;
    htmlUrl: string;
    type: string;
    folderName?: string;
}

export interface FolderType {
    id: string;
    name: string;
}

export interface UserType {
    id: string;
    username: string;
    email: string;
}


