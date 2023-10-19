//
//  LoginResponse.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 19.10.2023.
//

import Foundation

public struct LoginResponse: Codable {
    
    private enum CodingKeys: String, CodingKey {
        case authorizationCode = "authorization_code"
    }
    
    var authorizationCode: String
}
