using ParkingMate.Blockchain.Infrastructure;
using ParkingMate.BlockchainService.Dtos;
using System.Text.Json;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// B3: state + gate + storage
builder.Services.AddSingleton<BlockchainStorage>();
builder.Services.AddSingleton<BlockchainState>();
builder.Services.AddSingleton<MiningGate>();

var app = builder.Build();

// TASK D1 � GLOBAL ERROR HANDLER (OVDE)
app.UseExceptionHandler(errorApp =>
{
    errorApp.Run(async context =>
    {
        context.Response.StatusCode = 500;
        context.Response.ContentType = "application/json";

        var payload = new ErrorResponseDto
        {
            ErrorCode = "INTERNAL_ERROR",
            Message = "Unexpected server error"
        };

        await context.Response.WriteAsync(
            JsonSerializer.Serialize(payload)
        );
    });
});

app.UseSwagger();
app.UseSwaggerUI();

app.MapControllers();

app.MapGet("/health", () => Results.Ok(new { status = "ok" }));

app.Run();
