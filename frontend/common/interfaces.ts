export interface User {
  userId: number;
  login: string;
  fullname: string;
  isonline: boolean;
};

export interface Locales {
  paramKey: string;
  paramValue: string; 
  language: string;
};
