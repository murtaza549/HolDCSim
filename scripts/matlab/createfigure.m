function createfigure(X1, Y1, X2, Y2)
%CREATEFIGURE(X1, Y1, X2, Y2)
%  X1:  vector of x data
%  Y1:  vector of y data
%  X2:  vector of x data
%  Y2:  vector of y data

%  Auto-generated by MATLAB on 27-Apr-2018 12:40:16

% Create figure
figure;

% Create axes
axes1 = axes('Position',[0.13 0.11 0.775 0.860769230769231]);
hold(axes1,'on');

% Create plot
plot(X1,Y1,'DisplayName','Global Foresight','LineWidth',3,...
    'Color',[0 0.498039215803146 0]);

% Create plot
plot(X2,Y2,'DisplayName','Server-Balanced','LineWidth',3,'LineStyle',':');

% Create xlabel
xlabel('Job Latency (sec)','FontWeight','bold');

% Create ylabel
ylabel('CDF','FontWeight','bold');

box(axes1,'on');
% Set the remaining axes properties
set(axes1,'FontSize',34,'FontWeight','bold');
% Create legend
legend1 = legend(axes1,'show');
set(legend1,...
    'Position',[0.666753466448023 0.195982606383111 0.216666666666667 0.266966210042416],...
    'FontSize',34);
