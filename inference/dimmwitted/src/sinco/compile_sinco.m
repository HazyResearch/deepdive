% Script to compile mex files for SINCO using MATLAB interface
%
%
% Caution!!!! Only tested for Linux right now. 

% Test for mex extension to determine matlab version and platform
switch mexext
    case {'mexw32'} % Win32 MATLAB after 7.1
        files='sinco.cpp sinco_mex.cpp';
%        libs=[matlabroot,'\extern\lib\win32\microsoft\libmwlapack.lib'];
        switches='-v -O -DWIN32';
        eval(['mex ',switches,' ',files,' ','''']);
    case {'dll'} % Win32 MATLAB before 7.1
        files='sinco.cpp sinco_mex.cpp';
%        libs=[matlabroot,'\extern\lib\win32\microsoft\msvc60\libmwlapack.lib'];
        switches='-v -O -DWIN32';
        eval(['mex ',switches,' ',files,' ','''']);
    case {'mexmac'}% Macintosh using the VecLib framework
        files='cinco.cpp sinco_mex.cpp';
        switches='-v -O -Dmac';
        eval(['mex ',switches,' ',files,' ']);
    otherwise % All other platforms
        files='sinco.cpp sinco_mex.cpp';
        switches='-v -O -Dlinuxp';
        eval(['mex ',switches,' ',files,' ']);
end
disp(' ......................... Done Compiling Source .........................')
