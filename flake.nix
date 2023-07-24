{
  description = "JET";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
    clj-nix = {
      url = "/Users/slim/slimslenderslacks/clj-nix";
      inputs.flake-utils.follows = "flake-utils";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    devshell = {
      url = "github:numtide/devshell";
      inputs.flake-utils.follows = "flake-utils";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs = { self, nixpkgs, flake-utils, clj-nix, devshell }:

    flake-utils.lib.eachDefaultSystem (system:
      let
        overlays = [
          devshell.overlays.default
          (self: super: {
            clj-nix = clj-nix.packages."${system}";
          })
        ];
        pkgs = import nixpkgs {
          inherit overlays system;
        };
      in
      {
        packages = rec {
          clj = pkgs.clj-nix.mkCljBin {
            name = "jet-clj";
            jdkRunner = pkgs.graalvmCEPackages.graalvm19-ce;
            projectSrc = ./.;
            main-ns = "jet.main";
            buildCommand = "clj -T:build uber";
          };
          default = pkgs.clj-nix.mkGraalBin {
            name = "jet";
            cljDrv = self.packages."${system}".clj;
            graalvmXmx = "-J-Xmx3g -J-Djet.native=true";
            extraNativeImageBuildArgs = [
              "-H:+ReportExceptionStackTraces"
              "--verbose"
              "--no-fallback"
              "--no-server"
            ];
          };
        };

        devShells.default = pkgs.devshell.mkShell {
          name = "jet-devshell";
          packages = with pkgs; [ babashka clojure leiningen graalvmCEPackages.graalvm19-ce ];

          commands = [
            {
              name = "lock-clojure-deps";
              help = "update deps-lock.json whenever deps.edn changes";
              command = "nix run /Users/slim/slimslenderslacks/clj-nix#deps-lock";
            }
          ];
        };
      });
}
